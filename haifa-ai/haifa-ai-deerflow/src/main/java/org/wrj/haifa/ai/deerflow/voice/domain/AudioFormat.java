package org.wrj.haifa.ai.deerflow.voice.domain;

public record AudioFormat(
        String codec,
        int sampleRateHz,
        int channels,
        int bitsPerSample,
        int chunkDurationMs
) {
    public static final AudioFormat DEFAULT_INPUT = new AudioFormat("pcm_s16le", 16000, 1, 16, 100);
    public static final AudioFormat DEFAULT_OUTPUT = new AudioFormat("pcm_s16le", 24000, 1, 16, 100);

    public boolean isValidInput() {
        return "pcm_s16le".equalsIgnoreCase(codec) && sampleRateHz > 0 && channels == 1 && bitsPerSample == 16;
    }
}
