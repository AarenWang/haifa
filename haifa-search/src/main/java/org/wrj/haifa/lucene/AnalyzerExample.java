package org.wrj.haifa.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.io.StringReader;

public class AnalyzerExample {

    static String enText = "If you want to use a particular combination of CharFilters, a Tokenizer, and some TokenFilters, the simplest thing is often an create an anonymous subclass of Analyzer, provide Analyzer.createComponents(String) and perhaps also Analyzer.initReader(String, java.io.Reader). However, if you need the same set of components over and over in many places, you can make a subclass of Analyzer. In fact, Apache Lucene supplies a large family of Analyzer classes that deliver useful analysis chains. The most common of these is the StandardAnalyzer. Many applications will have a long and industrious life with nothing more than the StandardAnalyzer. The analyzers-common library provides many pre-existing analyzers for various languages. The analysis-common library also allows to configure a custom Analyzer without subclassing using the CustomAnalyzer class.\n";
    static String cnText = "";
    public static void main(String[] args) {

        Version matchVersion = Version.LUCENE_8_9_0; // Substitute desired Lucene version for XY
        Analyzer analyzer = new StandardAnalyzer(); // or any other analyzer
        analyzer.setVersion(matchVersion);
        TokenStream ts = analyzer.tokenStream("context", new StringReader(enText));
        // The Analyzer class will construct the Tokenizer, TokenFilter(s), and CharFilter(s),
        //   and pass the resulting Reader to the Tokenizer.
        OffsetAttribute offsetAtt = ts.addAttribute(OffsetAttribute.class);
        CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);


        try {
            ts.reset(); // Resets this stream to the beginning. (Required)
            while (ts.incrementToken()) {
                // Use AttributeSource.reflectAsString(boolean)
                // for token stream debugging.
                System.out.println("token: " + ts.reflectAsString(true));
                System.out.println("token start offset: " + offsetAtt.startOffset());
                System.out.println("  token end offset: " + offsetAtt.endOffset());

                System.out.println(termAtt.toString());
            }
            ts.end();   // Perform end-of-stream operations, e.g. set the final offset.
        }catch (IOException e){
            e.printStackTrace();
        }
        finally {
            try {
                ts.close(); // Release resources associated with this stream.
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
