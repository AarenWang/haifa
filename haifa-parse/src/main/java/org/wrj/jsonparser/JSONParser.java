package org.wrj.jsonparser;

import org.wrj.jsonparser.parser.Parser;
import org.wrj.jsonparser.tokenizer.CharReader;
import org.wrj.jsonparser.tokenizer.TokenList;
import org.wrj.jsonparser.tokenizer.Tokenizer;

import java.io.IOException;
import java.io.StringReader;

/**
 * Created by code4wt on 17/9/1.
 */
public class JSONParser {

    private Tokenizer tokenizer = new Tokenizer();

    private Parser parser = new Parser();

    public Object fromJSON(String json) throws IOException {
        CharReader charReader = new CharReader(new StringReader(json));
        TokenList tokens = tokenizer.tokenize(charReader);
        return parser.parse(tokens);
    }
}
