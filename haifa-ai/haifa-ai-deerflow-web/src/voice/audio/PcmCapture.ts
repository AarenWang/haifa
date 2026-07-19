export type PcmFrameCallback = (pcmBytes: Uint8Array, sequence: number) => void;

const WORKLET_SOURCE = `
class HaifaPcmCaptureProcessor extends AudioWorkletProcessor {
  constructor(options) {
    super();
    this.targetRate = options.processorOptions.targetRate || 16000;
    this.targetFrameSamples = options.processorOptions.frameSamples || 1600;
    this.source = [];
    this.output = [];
    this.position = 0;
  }
  process(inputs) {
    const channel = inputs[0] && inputs[0][0];
    if (!channel || channel.length === 0) return true;
    for (let i = 0; i < channel.length; i++) this.source.push(channel[i]);
    const ratio = sampleRate / this.targetRate;
    while (this.position + 1 < this.source.length) {
      const index = Math.floor(this.position);
      const fraction = this.position - index;
      const sample = this.source[index] * (1 - fraction) + this.source[index + 1] * fraction;
      this.output.push(sample);
      this.position += ratio;
      if (this.output.length >= this.targetFrameSamples) {
        const pcm = new Int16Array(this.targetFrameSamples);
        for (let j = 0; j < pcm.length; j++) {
          const value = Math.max(-1, Math.min(1, this.output[j]));
          pcm[j] = value < 0 ? value * 32768 : value * 32767;
        }
        this.output.splice(0, this.targetFrameSamples);
        this.port.postMessage(pcm.buffer, [pcm.buffer]);
      }
    }
    const consumed = Math.floor(this.position);
    if (consumed > 0) {
      this.source.splice(0, consumed);
      this.position -= consumed;
    }
    return true;
  }
}
registerProcessor('haifa-pcm-capture', HaifaPcmCaptureProcessor);
`;

export class PcmCapture {
  private mediaStream: MediaStream | null = null;
  private audioContext: AudioContext | null = null;
  private sourceNode: MediaStreamAudioSourceNode | null = null;
  private workletNode: AudioWorkletNode | null = null;
  private silentGain: GainNode | null = null;
  private moduleUrl: string | null = null;
  private sequence = 0;

  public async start(onFrame: PcmFrameCallback): Promise<void> {
    this.stop();
    this.sequence = 0;
    this.mediaStream = await navigator.mediaDevices.getUserMedia({
      audio: {
        channelCount: 1,
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true,
      },
    });

    try {
      this.audioContext = new AudioContext();
      this.moduleUrl = URL.createObjectURL(new Blob([WORKLET_SOURCE], { type: 'text/javascript' }));
      await this.audioContext.audioWorklet.addModule(this.moduleUrl);
      this.sourceNode = this.audioContext.createMediaStreamSource(this.mediaStream);
      this.workletNode = new AudioWorkletNode(this.audioContext, 'haifa-pcm-capture', {
        numberOfInputs: 1,
        numberOfOutputs: 1,
        outputChannelCount: [1],
        processorOptions: { targetRate: 16000, frameSamples: 1600 },
      });
      this.silentGain = this.audioContext.createGain();
      this.silentGain.gain.value = 0;
      this.workletNode.port.onmessage = (event: MessageEvent<ArrayBuffer>) => {
        this.sequence += 1;
        onFrame(new Uint8Array(event.data), this.sequence);
      };
      this.sourceNode.connect(this.workletNode);
      this.workletNode.connect(this.silentGain);
      this.silentGain.connect(this.audioContext.destination);
      await this.audioContext.resume();
    } catch (error) {
      this.stop();
      throw error;
    }
  }

  public stop(): void {
    this.workletNode?.disconnect();
    this.sourceNode?.disconnect();
    this.silentGain?.disconnect();
    this.workletNode = null;
    this.sourceNode = null;
    this.silentGain = null;
    this.mediaStream?.getTracks().forEach((track) => track.stop());
    this.mediaStream = null;
    void this.audioContext?.close();
    this.audioContext = null;
    if (this.moduleUrl) URL.revokeObjectURL(this.moduleUrl);
    this.moduleUrl = null;
  }
}
