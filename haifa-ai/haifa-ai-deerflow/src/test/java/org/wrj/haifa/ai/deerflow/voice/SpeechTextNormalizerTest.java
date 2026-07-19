package org.wrj.haifa.ai.deerflow.voice;

import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.voice.application.SpeechTextNormalizer;

import static org.junit.jupiter.api.Assertions.*;

public class SpeechTextNormalizerTest {

    @Test
    public void testMarkdownCodeBlockReplacement() {
        String input = "以下是代码示例：\n```python\nprint('hello')\n```\n请查收。";
        String normalized = SpeechTextNormalizer.normalize(input);
        assertTrue(normalized.contains("已生成代码，可在界面查看。"));
        assertFalse(normalized.contains("print('hello')"));
    }

    @Test
    public void testMarkdownUrlAndHeaderFormatting() {
        String input = "### 标题说明\n请访问 [DeerFlow 文档](https://example.com/doc) 查看 **核心架构**。";
        String normalized = SpeechTextNormalizer.normalize(input);
        assertFalse(normalized.contains("###"));
        assertFalse(normalized.contains("**"));
        assertTrue(normalized.contains("请访问 DeerFlow 文档 查看 核心架构"));
    }
}
