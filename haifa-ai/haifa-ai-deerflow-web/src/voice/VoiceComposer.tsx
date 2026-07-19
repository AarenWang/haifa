import type { CSSProperties } from 'react';
import { Mic, Power, Square } from 'lucide-react';
import { useVoiceConversation, type VoiceState } from './useVoiceConversation';

interface VoiceComposerProps {
  threadId?: string;
  onThreadReady?: (threadId: string) => void;
  onTurnComplete?: (threadId?: string, runId?: string) => void;
}

export function VoiceComposer({ threadId, onThreadReady, onTurnComplete }: VoiceComposerProps) {
  const { state, transcript, agentText, errorMsg, connect, startListening, commitTurn, cancelTurn } =
    useVoiceConversation({
      threadId,
      onThreadReady,
      onAgentComplete: (_text, completedThreadId, runId) => onTurnComplete?.(completedThreadId, runId),
    });

  const handlePrimaryAction = () => {
    if (state === 'IDLE' || state === 'ERROR') void connect();
    else if (state === 'READY') void startListening();
    else if (state === 'LISTENING') commitTurn();
  };

  const statusText: Record<VoiceState, string> = {
    IDLE: '点击启用语音对话',
    CONNECTING: '正在连接语音服务…',
    READY: '麦克风已就绪，点击后开始说话',
    LISTENING: '🎙️ 正在录音中… (再次点击提交)',
    TRANSCRIBING: '正在识别语音…',
    WAITING_AGENT: 'DeerFlow 正在思考…',
    SPEAKING: 'DeerFlow 正在朗读…',
    ERROR: errorMsg || '语音会话发生错误',
  };

  const primaryDisabled = state === 'CONNECTING' || state === 'TRANSCRIBING'
    || state === 'WAITING_AGENT' || state === 'SPEAKING';
  const canStop = state === 'LISTENING' || state === 'TRANSCRIBING'
    || state === 'WAITING_AGENT' || state === 'SPEAKING';

  return (
    <section style={styles.container} aria-label="语音对话">
      <div style={styles.statusBadge} aria-live="polite">
        <span
          className={state === 'LISTENING' ? 'voice-recording-dot' : ''}
          style={statusIndicator(state)}
        />
        <span style={styles.statusLabel}>{statusText[state]}</span>
      </div>

      {(transcript || agentText) && (
        <div style={styles.transcriptBox}>
          {transcript && <div style={styles.userText}>你：{transcript}</div>}
          {agentText && <div style={styles.agentText}>DeerFlow：{agentText}</div>}
        </div>
      )}

      <div style={styles.controls}>
        <button
          type="button"
          onClick={handlePrimaryAction}
          disabled={primaryDisabled}
          className={state === 'LISTENING' ? 'voice-recording-btn' : ''}
          style={{
            ...styles.primaryButton,
            ...(state === 'LISTENING' ? styles.recordingButton : {}),
            ...(state === 'READY' ? styles.readyButton : {}),
            ...(state === 'ERROR' ? styles.errorButton : {}),
          }}
        >
          {state === 'IDLE' || state === 'ERROR' ? <Power size={16} />
            : state === 'LISTENING' ? <Square size={16} /> : <Mic size={16} />}
          {state === 'IDLE' ? '连接'
            : state === 'ERROR' ? '重试连接'
            : state === 'LISTENING' ? '完成' : '说话'}
        </button>

        {canStop && (
          <button type="button" onClick={cancelTurn} style={styles.stopButton}>
            <Square size={15} />
            停止
          </button>
        )}
      </div>
      <small style={styles.privacyNote}>麦克风音频默认不保存，仅用于本轮实时识别。</small>
    </section>
  );
}

const styles: Record<string, CSSProperties> = {
  container: {
    display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '12px 16px',
    background: '#1e1e24', borderRadius: '12px', border: '1px solid #333', color: '#fff',
    gap: '10px', maxWidth: '560px', margin: '10px auto',
  },
  statusBadge: { display: 'flex', alignItems: 'center', gap: '8px', fontSize: '14px' },
  statusLabel: { color: '#d9d9d9' },
  transcriptBox: {
    width: '100%', padding: '8px 12px', background: '#2a2a32', borderRadius: '8px',
    fontSize: '13px', lineHeight: 1.5, maxHeight: '160px', overflow: 'auto',
  },
  userText: { color: '#69b1ff', marginBottom: '4px' },
  agentText: { color: '#95de64' },
  controls: { display: 'flex', gap: '12px' },
  primaryButton: {
    display: 'inline-flex', alignItems: 'center', gap: '6px', padding: '8px 20px', borderRadius: '20px',
    border: 'none', background: '#1677ff', color: '#fff', fontWeight: 600, cursor: 'pointer',
    transition: 'all 0.3s ease',
  },
  readyButton: { background: '#1677ff' },
  recordingButton: { background: '#ff4d4f' },
  errorButton: { background: '#d9363e' },
  stopButton: {
    display: 'inline-flex', alignItems: 'center', gap: '6px', padding: '8px 16px', borderRadius: '20px',
    border: 'none', background: '#434343', color: '#fff', cursor: 'pointer',
  },
  privacyNote: { color: '#8c8c8c' },
};

function statusIndicator(state: VoiceState): CSSProperties {
  const color = state === 'LISTENING' ? '#ff4d4f'
    : state === 'SPEAKING' ? '#52c41a'
      : state === 'TRANSCRIBING' || state === 'WAITING_AGENT' || state === 'CONNECTING' ? '#faad14'
        : state === 'READY' ? '#1677ff' : state === 'ERROR' ? '#ff7875' : '#8c8c8c';
  return { width: '10px', height: '10px', borderRadius: '50%', backgroundColor: color, transition: 'all 0.3s ease' };
}
