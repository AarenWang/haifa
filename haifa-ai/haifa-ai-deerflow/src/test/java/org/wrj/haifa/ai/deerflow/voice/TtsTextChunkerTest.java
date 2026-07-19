package org.wrj.haifa.ai.deerflow.voice;

import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.voice.application.TtsTextChunker;
import org.wrj.haifa.ai.deerflow.voice.domain.TextChunk;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TtsTextChunkerTest {

    @Test
    public void testFirstChunkFastStartAndPunctuationCut() {
        TtsTextChunker chunker = new TtsTextChunker();
        List<TextChunk> chunks = chunker.offer("你好，这是 DeerFlow 多模态语音系统。包含流式识别与实时朗读。");
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.get(0).text().contains("你好，这是 DeerFlow 多模态语音系统。"));
    }

    @Test
    public void testFlushRemainingText() {
        TtsTextChunker chunker = new TtsTextChunker();
        chunker.offer("短文本");
        List<TextChunk> flushed = chunker.flush();
        assertEquals(1, flushed.size());
        assertEquals("短文本", flushed.get(0).text());
        assertTrue(flushed.get(0).endOfSegment());
    }
}
