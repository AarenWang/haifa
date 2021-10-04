package org.wrj.haifa.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.io.StringReader;

public class CnAnalyzer {

    static String cnText = "按照美国总统拜登的计划，美军正在加紧行动，争取在8月31日前完全从阿富汗撤出。不过，美国似乎也并不甘心就这样在南亚地区留下权力真空。 欧洲“现代外交”网站近日刊文指出，美军撤离阿富汗后，巴基斯坦承受着巨大压力，美国正在要求其向阿富汗难民开放边境，并向美军提供军事基地，为自身的失败充当“替罪羊”。";
    public static void main(String[] args) {

        Version matchVersion = Version.LUCENE_8_9_0; // Substitute desired Lucene version for XY
        Analyzer analyzer = new SmartChineseAnalyzer(); // or any other analyzer
        analyzer.setVersion(matchVersion);

        TokenStream stream = analyzer.tokenStream("context", new StringReader(cnText));
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
        OffsetAttribute offsetAtt = stream.addAttribute(OffsetAttribute.class);

        try {
            stream.reset();
            while (stream.incrementToken()) {
                System.out.println(termAtt.toString()+":"+offsetAtt.startOffset()+":"+offsetAtt.endOffset());
            }
            stream.end();
        }catch (IOException e){
            e.printStackTrace();
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
