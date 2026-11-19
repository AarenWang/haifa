package org.wrj.haifa.ai.deerflow.voice.domain;

public record AudioChunk(
        long sequence,
        byte[] data,
        AudioFormat format
) {
    public AudioChunk {
        data = data == null ? new byte[0] : data.clone();
        format = format == null ? AudioFormat.DEFAULT_INPUT : format;
    }

    @Override
    public byte[] data() { return data.clone(); }
}
