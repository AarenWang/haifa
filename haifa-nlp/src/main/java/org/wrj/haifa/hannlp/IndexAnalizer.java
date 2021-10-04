package org.wrj.haifa.hannlp;

import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.IndexTokenizer;

import java.util.List;

public class IndexAnalizer {

    private static final String TEXT = "紧急重要】【网络异常：已恢复】尊敬的腾讯云用户，您好！\n" +
            "故障描述: 腾讯云监控到， 上海与多省的内网互访可能有抖动，该故障目前已经恢复（抖动时间 08/28 22:15:02~08/28 22:15:32 (GMT+8)），平台持续关注中，谢谢您对腾讯云的信赖与支持。\n" +
            "\n" +
            "腾讯云提示: 我们非常关注您的业务是否仍有影响，希望您尽快评估业务情况，您的反馈和意见对我们非常重要，我们会在本群 7*24h 伴随，非常感谢您与腾讯云一同成长！";

    public static void main(String[] args) {
        List<Term> termList = IndexTokenizer.segment(TEXT);
        for (Term term : termList)
        {
            System.out.println(term + " [" + term.offset + ":" + (term.offset + term.word.length()) + "]");
        }
    }
}
