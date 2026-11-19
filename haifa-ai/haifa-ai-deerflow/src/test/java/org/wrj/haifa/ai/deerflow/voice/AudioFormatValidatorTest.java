package org.wrj.haifa.ai.deerflow.voice;

import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.voice.domain.AudioFormat;

import static org.junit.jupiter.api.Assertions.*;

public class AudioFormatValidatorTest {

    @Test
    public void testDefaultInputFormatValidation() {
        AudioFormat input = AudioFormat.DEFAULT_INPUT;
        assertTrue(input.isValidInput());
        assertEquals("pcm_s16le", input.codec());
        assertEquals(16000, input.sampleRateHz());
        assertEquals(1, input.channels());
        assertEquals(16, input.bitsPerSample());
    }

    @Test
    public void testInvalidInputFormat() {
        AudioFormat invalid = new AudioFormat("mp3", 44100, 2, 16, 100);
        assertFalse(invalid.isValidInput());
    }
}
