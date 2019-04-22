package org.wrj.haifa.dance;

import org.junit.Test;

public class StringAlgorithmTest {

    @Test
    public void testMaxUnRepeatCharacter(){
        System.out.println(StringAlgorithm.maxUnRepeatCharacter("abcabcbb"));

    }

    @Test
    public void testLongestCommonPrefix(){
        System.out.println(StringAlgorithm.longestCommonPrefix(new  String[]{"flower","flow","flight"}));

    }

    @Test
    public void testCheckInclusion(){
        System.out.println(StringAlgorithm.checkInclusion(null,null));
        System.out.println(StringAlgorithm.checkInclusion(null,""));
        System.out.println(StringAlgorithm.checkInclusion("",null));
        System.out.println(StringAlgorithm.checkInclusion("",""));
        System.out.println(StringAlgorithm.checkInclusion("ab","abcd"));
        System.out.println(StringAlgorithm.checkInclusion("cd","abcd"));
        System.out.println(StringAlgorithm.checkInclusion("cde","abcd"));
        System.out.println(StringAlgorithm.checkInclusion("bc","abcd"));
        System.out.println(StringAlgorithm.checkInclusion("1ab","abcd"));

    }

    @Test
    public void testSimplifyPath(){
        StringAlgorithm.simplifyPath("\"/home/\"");
    }
}
