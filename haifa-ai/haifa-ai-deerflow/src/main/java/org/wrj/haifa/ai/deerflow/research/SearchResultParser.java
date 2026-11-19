package org.wrj.haifa.ai.deerflow.research;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SearchResultParser {

    private static final Pattern RESULT_PATTERN = Pattern.compile(
            "(?m)^\\s*\\d+\\.\\s*\\[(.+?)]\\s+(https?://\\S+)\\s*$\\s*^\\s*(?:Summary:)?\\s*(.+?)\\s*$");

    public List<SearchResultCandidate> parse(String rawResult) {
        List<SearchResultCandidate> candidates = new ArrayList<>();
        if (!StringUtils.hasText(rawResult)) {
            return candidates;
        }
        Matcher matcher = RESULT_PATTERN.matcher(rawResult);
        while (matcher.find()) {
            candidates.add(new SearchResultCandidate(
                    matcher.group(1).trim(),
                    matcher.group(2).trim(),
                    matcher.group(3).trim()
            ));
        }
        return candidates;
    }
}
