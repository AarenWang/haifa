package org.wrj.haifa.regex;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTest {


    public static void main(String[] args) {


    }


    @Test
    public void test1(){
        String str = "我是[001]真心求救的[002]，你能帮帮我吗";

        Pattern pattern = Pattern.compile("\\[(.*?)\\]");

        Matcher matcher = pattern.matcher(str);

        while(matcher.find()){
            System.out.println(matcher.group(1));

        }
    }



    @Test
    public  void  test2() throws IOException {
        Path jsPath = Paths.get("/Users/wangrenjun/wedoctor_project/sm-trd-project/hx-trd-web-deal/1.txt");
        byte[] allBytes = Files.readAllBytes(jsPath);
        String content = new String(allBytes);

        System.out.println(content.length());



        //Pattern pattern = Pattern.compile("requires\":\\[$(^*$)+\\^]$",Pattern.MULTILINE | Pattern.DOTALL);
        //Pattern pattern = Pattern.compile("\"requires\":\\[(.)+?\\]",Pattern.DOTALL);
        Pattern pattern = Pattern.compile("\"requires\":\\[(.*?)\\]",Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        //boolean find = matcher.find();
        int groupCount = matcher.groupCount();

        //System.out.printf("find %b match,groupCount=%d \r\n",find,groupCount);
        while (matcher.find()) {
            System.out.println(matcher.group(1));
        }

    }

    @Test
    public  void  test3() throws IOException {
        Path jsPath = Paths.get("/Users/wangrenjun/wedoctor_project/sm-trd-project/hx-trd-web-deal/1.txt");
        byte[] allBytes = Files.readAllBytes(jsPath);
        String content = new String(allBytes);
        System.out.println(content.length());


        String replaceContent = content.replaceAll("\"requires\":\\[(.*?)\\]","A");


        System.out.println(replaceContent);
    }

    @Test
    public void test4() throws IOException {
        //   /Users/wangrenjun/ext_js/Dx.trd.manufacturer.ManufactureDisApplyPanel

        Path jsPath = Paths.get("/Users/wangrenjun/ext_js/Dx.trd.manufacturer.ManufactureDisApplyPanel");
        byte[] allBytes = Files.readAllBytes(jsPath);
        String content = new String(allBytes);
        Pattern pattern = Pattern.compile("\"requires\":\\[(.*?)\\]");

        Matcher matcher = pattern.matcher(content);
        System.out.println(matcher.find());

        System.out.println(matcher.group());

        String group = matcher.group();

        String requiresArrayStr = group.substring(group.indexOf("[")+1,group.lastIndexOf("]"));
        System.out.println(requiresArrayStr);

        String[] splitStr = requiresArrayStr.split(",");
        for(String str: splitStr) {
            str = str.replaceAll("\"","");
            System.out.println(str);
        }

    }

    @Test
    public void  test5() {
        String dxName = "Dx.trd.manufacturer.ManufactureDisApplyPanel";

        dxName = dxName.substring(dxName.indexOf(".")+1).replaceAll("\\.","/")+".js";
        System.out.println(dxName);
        Assert.assertTrue(dxName.equals("trd/manufacturer/ManufactureDisApplyPanel.js"));

    }


}
