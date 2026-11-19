export class PcmPlayer {
  private sampleRate: number;
  private audioContext: AudioContext | null = null;
  private nextStartTime = 0;
  private nextSequence = 1;
  private readonly pending = new Map<number, Uint8Array>();
  private readonly activeSources = new Set<AudioBufferSourceNode>();

  constructor(sampleRate = 24000) {
    this.sampleRate = sampleRate;
  }

  public setSampleRate(sampleRate: number): void {
    if (Number.isFinite(sampleRate) && sampleRate >= 8000 && sampleRate <= 48000 && sampleRate !== this.sampleRate) {
      this.stop();
      this.sampleRate = sampleRate;
    }
  }

  public feed(pcm16Bytes: Uint8Array, sequence: number): void {
    if (sequence < this.nextSequence || pcm16Bytes.byteLength === 0 || pcm16Bytes.byteLength % 2 !== 0) return;
    this.pending.set(sequence, pcm16Bytes.slice());
    while (this.pending.has(this.nextSequence)) {
      const next = this.pending.get(this.nextSequence)!;
      this.pending.delete(this.nextSequence);
      this.schedule(next);
      this.nextSequence += 1;
    }
  }

  public stop(): void {
    this.pending.clear();
    this.activeSources.forEach((source) => {
      try { source.stop(); } catch { /* already stopped */ }
      source.disconnect();
    });
    this.activeSources.clear();
    void this.audioContext?.close();
    this.audioContext = null;
    this.nextStartTime = 0;
    this.nextSequence = 1;
  }

  private schedule(pcm16Bytes: Uint8Array): void {
    if (!this.audioContext) {
      this.audioContext = new AudioContext();
      this.nextStartTime = this.audioContext.currentTime + 0.08;
      void this.audioContext.resume();
    }
    const context = this.audioContext;
    const view = new DataView(pcm16Bytes.buffer, pcm16Bytes.byteOffset, pcm16Bytes.byteLength);
    const floatSamples = new Float32Array(pcm16Bytes.byteLength / 2);
    for (let i = 0; i < floatSamples.length; i++) {
      const value = view.getInt16(i * 2, true);
      floatSamples[i] = value / (value < 0 ? 32768 : 32767);
    }
    const buffer = context.createBuffer(1, floatSamples.length, this.sampleRate);
    buffer.copyToChannel(floatSamples, 0);
    const source = context.createBufferSource();
    source.buffer = buffer;
    source.connect(context.destination);
    source.onended = () => {
      this.activeSources.delete(source);
      source.disconnect();
    };
    this.activeSources.add(source);
    this.nextStartTime = Math.max(this.nextStartTime, context.currentTime);
    source.start(this.nextStartTime);
    this.nextStartTime += buffer.duration;
  }
}
