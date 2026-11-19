package org.wrj.haifa.ai.deerflow.voice.application;

import org.wrj.haifa.ai.deerflow.voice.domain.TextChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TtsTextChunker {

    private final StringBuilder buffer = new StringBuilder();
    private final AtomicInteger sequenceCounter = new AtomicInteger(1);
    private boolean isFirstChunk = true;

    private static final int INITIAL_CHUNK_MIN_LEN = 20;
    private static final int STANDARD_CHUNK_MIN_LEN = 80;
    private static final int HARD_CHUNK_MAX_LEN = 160;

    public synchronized List<TextChunk> offer(String delta) {
        List<TextChunk> result = new ArrayList<>();
        if (delta == null || delta.isEmpty()) {
            return result;
        }

        buffer.append(delta);
        int targetMinLen = isFirstChunk ? INITIAL_CHUNK_MIN_LEN : STANDARD_CHUNK_MIN_LEN;

        while (true) {
            String current = buffer.toString();
            if (current.isEmpty()) break;

            int cutIndex = findCutIndex(current, targetMinLen, HARD_CHUNK_MAX_LEN);
            if (cutIndex <= 0) {
                break;
            }

            String chunkText = current.substring(0, cutIndex).trim();
            buffer.delete(0, cutIndex);

            if (!chunkText.isEmpty()) {
                result.add(new TextChunk(sequenceCounter.getAndIncrement(), chunkText, false));
                isFirstChunk = false;
                targetMinLen = STANDARD_CHUNK_MIN_LEN;
            }
        }

        return result;
    }

    public synchronized List<TextChunk> flush() {
        List<TextChunk> result = new ArrayList<>();
        String remaining = buffer.toString().trim();
        buffer.setLength(0);

        if (!remaining.isEmpty()) {
            result.add(new TextChunk(sequenceCounter.getAndIncrement(), remaining, true));
        }
        return result;
    }

    public synchronized void reset() {
        buffer.setLength(0);
        sequenceCounter.set(1);
        isFirstChunk = true;
    }

    private int findCutIndex(String text, int minLen, int maxLen) {
        if (text.length() >= maxLen) {
            // Cut at max len or nearest comma/space
            for (int i = maxLen; i >= minLen; i--) {
                char c = text.charAt(i - 1);
                if (isPunctuation(c)) {
                    return i;
                }
            }
            return maxLen;
        }

        if (text.length() < minLen) {
            return -1;
        }

        // Search for sentence ending punctuation
        for (int i = minLen; i <= text.length(); i++) {
            char c = text.charAt(i - 1);
            if (isSentenceEndPunctuation(c)) {
                return i;
            }
        }

        return -1;
    }

    private boolean isSentenceEndPunctuation(char c) {
        return c == '。' || c == '！' || c == '？' || c == '；' || c == '\n';
    }

    private boolean isPunctuation(char c) {
        return isSentenceEndPunctuation(c) || c == '，' || c == ',' || c == ' ' || c == '、';
    }
}
