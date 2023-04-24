package org.wrj.base;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JavaRegexExample {

    String  bombTimeRegex = "\\d{1,2}(\\.\\d{2})?";


    @Test
    public void  testNumber(){

        Assertions.assertTrue( "33.00".matches(bombTimeRegex));
        Assertions.assertTrue( "33".matches(bombTimeRegex));
    }
}
