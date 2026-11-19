import { getUserId } from '../api/deerflowClient';

export interface ControlMessage {
  type: string;
  voiceSessionId?: string;
  turnId?: string;
  threadId?: string;
  text?: string;
  runId?: string;
  reason?: string;
  code?: string;
  message?: string;
  payload?: Record<string, unknown>;
}

export type VoiceControlListener = (message: ControlMessage) => void;
export type VoiceAudioListener = (pcmData: Uint8Array, sequence: number) => void;
export type VoiceConnectionListener = (connected: boolean) => void;

export class VoiceClient {
  private readonly url: string;
  private ws: WebSocket | null = null;
  private connectPromise: Promise<void> | null = null;
  private controlListeners = new Set<VoiceControlListener>();
  private audioListeners = new Set<VoiceAudioListener>();
  private connectionListeners = new Set<VoiceConnectionListener>();

  constructor(url = '/api/deerflow/voice/ws') {
    this.url = url;
  }

  public isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN;
  }

  public isClosed(): boolean {
    return !this.ws || this.ws.readyState === WebSocket.CLOSED || this.ws.readyState === WebSocket.CLOSING;
  }

  public connect(): Promise<void> {
    if (this.ws?.readyState === WebSocket.OPEN) return Promise.resolve();
    if (this.connectPromise) return this.connectPromise;

    this.connectPromise = new Promise((resolve, reject) => {
      const location = window.location;
      const baseUrl = this.url.startsWith('ws://') || this.url.startsWith('wss://')
        ? new URL(this.url)
        : new URL(`${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}${this.url}`);
      baseUrl.searchParams.set('userId', getUserId());

      const socket = new WebSocket(baseUrl.toString());
      this.ws = socket;
      socket.binaryType = 'arraybuffer';

      socket.onopen = () => {
        this.connectPromise = null;
        this.connectionListeners.forEach((listener) => listener(true));
        resolve();
      };
      socket.onerror = () => {
        this.connectPromise = null;
        reject(new Error('Voice WebSocket connection failed'));
      };
      socket.onclose = () => {
        this.connectPromise = null;
        this.ws = null;
        this.connectionListeners.forEach((listener) => listener(false));
      };
      socket.onmessage = (event) => this.handleMessage(event);
    });
    return this.connectPromise;
  }

  public onControl(listener: VoiceControlListener): () => void {
    this.controlListeners.add(listener);
    return () => this.controlListeners.delete(listener);
  }

  public onAudio(listener: VoiceAudioListener): () => void {
    this.audioListeners.add(listener);
    return () => this.audioListeners.delete(listener);
  }

  public onConnection(listener: VoiceConnectionListener): () => void {
    this.connectionListeners.add(listener);
    return () => this.connectionListeners.delete(listener);
  }

  public sendControl(message: ControlMessage): boolean {
    if (this.ws?.readyState !== WebSocket.OPEN) return false;
    this.ws.send(JSON.stringify(message));
    return true;
  }

  public sendAudioFrame(pcmBytes: Uint8Array, sequence: number): boolean {
    const socket = this.ws;
    if (socket?.readyState !== WebSocket.OPEN || socket.bufferedAmount > 256 * 1024) return false;
    if (pcmBytes.byteLength === 0 || pcmBytes.byteLength % 2 !== 0 || sequence <= 0) return false;

    const frame = new ArrayBuffer(16 + pcmBytes.byteLength);
    const view = new DataView(frame);
    view.setUint16(0, 1, false);
    view.setUint8(2, 1);
    view.setUint8(3, 1);
    view.setBigUint64(4, BigInt(sequence), false);
    view.setInt32(12, 0, false);
    new Uint8Array(frame, 16).set(pcmBytes);
    socket.send(frame);
    return true;
  }

  public close(): void {
    const socket = this.ws;
    this.ws = null;
    this.connectPromise = null;
    if (socket && socket.readyState < WebSocket.CLOSING) socket.close(1000, 'voice client closed');
  }

  private handleMessage(event: MessageEvent): void {
    if (typeof event.data === 'string') {
      try {
        const message = JSON.parse(event.data) as ControlMessage;
        if (typeof message.type === 'string') {
          this.controlListeners.forEach((listener) => listener(message));
        }
      } catch (error) {
        console.error('Failed to parse voice control message', error);
      }
      return;
    }
    if (!(event.data instanceof ArrayBuffer) || event.data.byteLength < 16) return;
    const view = new DataView(event.data);
    const version = view.getUint16(0, false);
    const direction = view.getUint8(2);
    const codec = view.getUint8(3);
    const sequence = Number(view.getBigUint64(4, false));
    const headerLength = view.getInt32(12, false);
    const payloadOffset = 16 + headerLength;
    if (version !== 1 || direction !== 2 || codec !== 1 || sequence <= 0
        || headerLength < 0 || payloadOffset >= event.data.byteLength) return;
    const pcmData = new Uint8Array(event.data.slice(payloadOffset));
    this.audioListeners.forEach((listener) => listener(pcmData, sequence));
  }
}
