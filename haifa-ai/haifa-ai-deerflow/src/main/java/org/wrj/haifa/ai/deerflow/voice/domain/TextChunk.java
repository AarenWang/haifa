package org.wrj.haifa.ai.deerflow.voice.domain;

public record TextChunk(
        int sequence,
        String text,
        boolean endOfSegment
) {
    public TextChunk {
        text = text == null ? "" : text;
    }
}
