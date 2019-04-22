package org.wrj.haifa.dance;

import java.util.Stack;

public class StringAlgorithm {

    /**
     * 给定一个字符串，请你找出其中不含有重复字符的 最长子串 的长度。
     * @param str
     * @return
     */




    public  static int maxUnRepeatCharacter(String str) {
        int count = 0;
        int maxCount = 0;
        int currentCount = 0;
        int beginPos = -1;
        for(int i = 0; i < str.length()-1; i++ ){
           if(str.charAt(i) != str.charAt(i+1)) {
               currentCount++;
               if(currentCount > maxCount) {
                   maxCount = currentCount;
               }
           } else {
               currentCount = 0;
           }
        }

        return maxCount;
    }

    public static String longestCommonPrefix(String[] strs) {

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < strs.length-1; i++){
            boolean jFound = true;
            int index = 0;
            for(int j = 0; ;j++){
                index = j;
                if(strs[i].length() < j || strs[i+i].length() < j) {
                    jFound = false;
                    break;
                }
                if(strs[i].charAt(j) != strs[i+1].charAt(j)) {
                    jFound = false;
                    break;
                }
            }
           if(jFound) {
               sb.append(strs[i].charAt(index));
           }
        }
        return sb.toString();
    }

    /**
     * 给定两个字符串 s1 和 s2，写一个函数来判断 s2 是否包含 s1 的排列。
     * 换句话说，第一个字符串的排列之一是第二个字符串的子串。
     *
     * @param s1
     * @param s2
     * @return
     */
    public static boolean checkInclusion(String s1, String s2) {
        if(s1 == null || s2 == null || s2.length() < s1.length()) {
            return false;
        }

        for(int i = 0; i <= s2.length()-1; i++){
            int j = 0;
            while (j < s1.length()) {
                int count = 0;
                if(s2.charAt(i+count) == s1.charAt(j+count)){
                    count++;
                    if(count == s1.length()) {
                        return true;
                    }
                } else {
                    break;
                }
                j++;
            }
        }
        return false;

    }

    /**
     * 基于栈来实现
     * @param path
     * @return
     */
    public static String simplifyPath(String path) {

        Stack<String> stack = new Stack<>();
        String[] subDir = path.split("/");
        for(int i =0; i < subDir.length;i++){
            if(subDir[i].equals("..")){
                if(stack.empty()) {
                  //不做处理
                } else {
                    stack.pop();
                }
            }else if(subDir[i].equals(".")){
                //不做处理
            } else {
                stack.push(subDir[i]);
            }
        }
        StringBuilder sb  = new StringBuilder();
        while (!stack.isEmpty()){
            sb.append(stack.pop());
            sb.append("/");
        }
        char[] chars = new char[sb.length()];
        for(int i = 0; i < sb.length();i++){
            chars[i] = sb.toString().charAt(sb.length() - i -1);
        }
        return new String(chars);
    }

}
