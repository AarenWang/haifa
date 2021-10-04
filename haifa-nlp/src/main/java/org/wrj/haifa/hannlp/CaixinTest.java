package org.wrj.haifa.hannlp;

import com.hankcs.hanlp.summary.TextRankSentence;
import org.apache.commons.collections.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class CaixinTest {

    private static final String PATH = "/Users/wangrenjun/git/haifa/src/main/java/org/wrj/haifa/hannlp/caixin.txt";

    public static void main(String[] args) {
        try {
            String content = Files.readAllLines(Paths.get(PATH)).stream().collect(Collectors.joining());
            System.out.println(content);

            List<String> sentenceList = TextRankSentence.getTopSentenceList(content, 1000);
            sentenceList.stream().forEach( e -> {
                System.out.println(e);
            });


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
