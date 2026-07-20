import { useCallback, useEffect, useRef, useState } from 'react';
import { VoiceClient, type ControlMessage } from './VoiceClient';
import { PcmCapture } from './audio/PcmCapture';
import { PcmPlayer } from './audio/PcmPlayer';

export type VoiceState =
  | 'IDLE'
  | 'CONNECTING'
  | 'READY'
  | 'LISTENING'
  | 'TRANSCRIBING'
  | 'WAITING_AGENT'
  | 'SPEAKING'
  | 'ERROR';

export interface VoiceTurnRecord {
  turnId: string;
  userText: string;
  agentText: string;
  timestamp: number;
}

export interface UseVoiceConversationOptions {
  threadId?: string;
  onThreadReady?: (threadId: string) => void;
  onUserTranscript?: (text: string) => void;
  onAgentDelta?: (delta: string) => void;
  onAgentComplete?: (text: string, threadId?: string, runId?: string) => void;
  autoConnect?: boolean;
  autoContinuous?: boolean;
}

export function useVoiceConversation(options: UseVoiceConversationOptions = {}) {
  const [state, setState] = useState<VoiceState>('IDLE');
  const [transcript, setTranscript] = useState('');
  const [agentText, setAgentText] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [turnsHistory, setTurnsHistory] = useState<VoiceTurnRecord[]>([]);

  const optionsRef = useRef(options);
  optionsRef.current = options;
  const clientRef = useRef<VoiceClient | null>(null);
  const captureRef = useRef<PcmCapture | null>(null);
  const playerRef = useRef<PcmPlayer | null>(null);
  const currentTurnIdRef = useRef<string | null>(null);
  const currentThreadIdRef = useRef<string | undefined>(options.threadId);
  const currentRunIdRef = useRef<string | undefined>(undefined);
  const transcriptRef = useRef('');
  const agentTextRef = useRef('');
  const asrReadyRef = useRef(false);
  const pendingFramesRef = useRef<Array<{ bytes: Uint8Array; sequence: number }>>([]);
  const disposedRef = useRef(false);
  const autoConnectAttemptedRef = useRef(false);

  // Helper to ensure PcmCapture & PcmPlayer are ready
  const getAudioHardware = useCallback(() => {
    if (!captureRef.current) captureRef.current = new PcmCapture();
    if (!playerRef.current) playerRef.current = new PcmPlayer(24000);
    return { capture: captureRef.current, player: playerRef.current };
  }, []);

  const handleControl = useCallback((message: ControlMessage) => {
    if (disposedRef.current) return;
    if (message.turnId && currentTurnIdRef.current && message.turnId !== currentTurnIdRef.current) {
      return;
    }

    switch (message.type) {
      case 'session.ready':
        currentThreadIdRef.current = message.threadId;
        if (message.threadId) optionsRef.current.onThreadReady?.(message.threadId);
        setState('READY');
        setErrorMsg(null);
        break;
      case 'asr.ready':
        asrReadyRef.current = true;
        flushPendingFrames();
        setState('LISTENING');
        break;
      case 'asr.partial': {
        const text = message.text || '';
        transcriptRef.current = text;
        setTranscript(text);
        setState('TRANSCRIBING');
        break;
      }
      case 'asr.final': {
        const text = message.text || '';
        transcriptRef.current = text;
        setTranscript(text);
        setState('WAITING_AGENT');
        if (text) optionsRef.current.onUserTranscript?.(text);
        break;
      }
      case 'agent.started':
        currentRunIdRef.current = message.runId;
        agentTextRef.current = '';
        setAgentText('');
        setState('WAITING_AGENT');
        break;
      case 'agent.delta': {
        const delta = message.text || '';
        agentTextRef.current += delta;
        setAgentText(agentTextRef.current);
        if (delta) optionsRef.current.onAgentDelta?.(delta);
        break;
      }
      case 'tts.started': {
        const sampleRate = Number(message.payload?.sampleRateHz);
        if (Number.isFinite(sampleRate) && playerRef.current) {
          playerRef.current.setSampleRate(sampleRate);
        }
        break;
      }
      case 'tts.error':
        setErrorMsg(message.message || '语音朗读失败，文本回答仍然可用。');
        break;
      case 'turn.completed': {
        captureRef.current?.stop();
        const userT = transcriptRef.current;
        const agentT = agentTextRef.current;
        if (userT || agentT) {
          setTurnsHistory((prev) => [
            ...prev,
            {
              turnId: currentTurnIdRef.current || `turn_${Date.now()}`,
              userText: userT,
              agentText: agentT,
              timestamp: Date.now(),
            },
          ]);
        }
        setState('READY');
        optionsRef.current.onAgentComplete?.(
          agentT,
          message.threadId || currentThreadIdRef.current,
          message.runId || currentRunIdRef.current,
        );
        currentTurnIdRef.current = null;
        if (optionsRef.current.autoContinuous && !disposedRef.current) {
          setTimeout(() => {
            if (!disposedRef.current) {
              void startListening();
            }
          }, 1200);
        }
        break;
      }
      case 'turn.cancelled':
        captureRef.current?.stop();
        playerRef.current?.stop();
        setState('READY');
        currentTurnIdRef.current = null;
        break;
      case 'error':
        if (message.code === 'VOICE_SESSION_CONFLICT') {
          setState('READY');
          setErrorMsg(null);
        } else {
          captureRef.current?.stop();
          playerRef.current?.stop();
          setState('ERROR');
          setErrorMsg(message.message || '语音会话发生错误。');
        }
        break;
    }
  }, []);

  const flushPendingFrames = () => {
    const voiceClient = clientRef.current;
    if (!voiceClient || !asrReadyRef.current) return;
    for (const frame of pendingFramesRef.current) {
      if (!voiceClient.sendAudioFrame(frame.bytes, frame.sequence)) {
        failForBackpressure();
        return;
      }
    }
    pendingFramesRef.current = [];
  };

  const failForBackpressure = () => {
    captureRef.current?.stop();
    playerRef.current?.stop();
    if (currentTurnIdRef.current) {
      clientRef.current?.sendControl({
        type: 'turn.cancel',
        turnId: currentTurnIdRef.current,
        reason: 'CLIENT_BACKPRESSURE',
      });
    }
    setState('ERROR');
    setErrorMsg('网络发送缓冲区已满，请检查网络后重试。');
  };

  // Helper to get or create VoiceClient with handlers attached
  const getOrCreateClient = useCallback(() => {
    if (clientRef.current && !clientRef.current.isClosed()) {
      return clientRef.current;
    }

    if (clientRef.current) {
      clientRef.current.close();
    }

    const client = new VoiceClient();
    clientRef.current = client;

    client.onControl((message) => handleControl(message));

    client.onAudio((pcmBytes, sequence) => {
      if (disposedRef.current) return;
      setState('SPEAKING');
      playerRef.current?.feed(pcmBytes, sequence);
    });

    client.onConnection((connected) => {
      if (disposedRef.current) return;
      if (!connected) {
        captureRef.current?.stop();
        playerRef.current?.stop();
        setState((prev) => (prev === 'IDLE' ? 'IDLE' : 'ERROR'));
        setErrorMsg('语音服务连接已断开，请点击重试。');
      }
    });

    return client;
  }, [handleControl]);

  const connect = useCallback(async () => {
    getAudioHardware();
    const client = getOrCreateClient();

    setState('CONNECTING');
    setErrorMsg(null);
    try {
      await client.connect();
      const sent = client.sendControl({
        type: 'session.create',
        threadId: optionsRef.current.threadId,
      });
      if (!sent) throw new Error('发送 session.create 失败');
    } catch (error) {
      setState('ERROR');
      setErrorMsg(error instanceof Error ? error.message : '语音服务连接失败，请重试。');
    }
  }, [getAudioHardware, getOrCreateClient]);

  // Mount effect: auto-connect if requested (default true)
  useEffect(() => {
    disposedRef.current = false;
    currentThreadIdRef.current = options.threadId;
    getAudioHardware();

    const autoConnect = options.autoConnect !== false;
    if (autoConnect && !autoConnectAttemptedRef.current) {
      autoConnectAttemptedRef.current = true;
      void connect();
    }

    return () => {
      disposedRef.current = true;
      captureRef.current?.stop();
      playerRef.current?.stop();
      clientRef.current?.close();
      clientRef.current = null;
      captureRef.current = null;
      playerRef.current = null;
    };
  }, [connect, getAudioHardware, options.autoConnect, options.threadId]);

  const cancelTurn = useCallback(() => {
    captureRef.current?.stop();
    playerRef.current?.stop();
    pendingFramesRef.current = [];
    const turnId = currentTurnIdRef.current;
    if (turnId) clientRef.current?.sendControl({ type: 'turn.cancel', turnId, reason: 'USER_CANCELLED' });
    currentTurnIdRef.current = null;
    setState('READY');
  }, []);

  const startListening = useCallback(async () => {
    // If speaking, transcribing, or waiting agent, interrupt/cancel current turn first
    if (state === 'SPEAKING' || state === 'WAITING_AGENT' || state === 'TRANSCRIBING' || state === 'LISTENING') {
      cancelTurn();
    }

    // Ensure connection
    if (!clientRef.current || clientRef.current.isClosed() || state === 'IDLE' || state === 'ERROR') {
      await connect();
    }

    const { capture, player } = getAudioHardware();
    const client = getOrCreateClient();

    const turnId = `turn_${Date.now()}`;
    currentTurnIdRef.current = turnId;
    currentRunIdRef.current = undefined;
    asrReadyRef.current = false;
    pendingFramesRef.current = [];
    transcriptRef.current = '';
    agentTextRef.current = '';
    player.stop();
    setTranscript('');
    setAgentText('');
    setErrorMsg(null);

    try {
      await capture.start((bytes, sequence) => {
        if (!asrReadyRef.current) {
          if (pendingFramesRef.current.length >= 30) {
            capture.stop();
            client.sendControl({ type: 'turn.cancel', turnId, reason: 'ASR_START_TIMEOUT' });
            setState('ERROR');
            setErrorMsg('语音识别服务启动超时，请重试。');
            return;
          }
          pendingFramesRef.current.push({ bytes, sequence });
          return;
        }
        if (!client.sendAudioFrame(bytes, sequence)) {
          capture.stop();
          client.sendControl({ type: 'turn.cancel', turnId, reason: 'CLIENT_BACKPRESSURE' });
          setState('ERROR');
          setErrorMsg('网络发送缓冲区已满，请检查网络后重试。');
        }
      });
      if (!client.sendControl({ type: 'turn.start', turnId })) {
        throw new Error('发送 turn.start 失败');
      }
      setState('CONNECTING');
    } catch (error) {
      capture.stop();
      currentTurnIdRef.current = null;
      setState('ERROR');
      setErrorMsg(`无法使用麦克风：${error instanceof Error ? error.message : '请检查麦克风权限'}`);
    }
  }, [cancelTurn, connect, getAudioHardware, getOrCreateClient, state]);

  const commitTurn = useCallback(() => {
    const turnId = currentTurnIdRef.current;
    if (state !== 'LISTENING' || !turnId) return;
    captureRef.current?.stop();
    pendingFramesRef.current = [];
    setState('TRANSCRIBING');
    clientRef.current?.sendControl({ type: 'turn.commit', turnId });
  }, [state]);

  const clearHistory = useCallback(() => {
    setTurnsHistory([]);
    setTranscript('');
    setAgentText('');
  }, []);

  return {
    state,
    transcript,
    agentText,
    turnsHistory,
    errorMsg,
    connect,
    startListening,
    commitTurn,
    cancelTurn,
    clearHistory,
  };
}

