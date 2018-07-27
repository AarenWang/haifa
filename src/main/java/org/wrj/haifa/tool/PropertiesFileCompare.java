package org.wrj.haifa.tool;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PropertiesFileCompare {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("arg1 is left file path arg2 is right file path");
            return;
        }


        Path left = Paths.get(args[0]);
        Path right = Paths.get(args[1]);

        if (!Files.exists(left) || !Files.isRegularFile(left) || !Files.isReadable(left)) {
            System.out.printf("file %s exists? %b ? ,isRegularFile? %b,isReadable? %b \r\n", left, Files.exists(left),
                    Files.isRegularFile(left), Files.isReadable(left));
            return;

        }

        if (!Files.exists(right) || !Files.isRegularFile(right) || !Files.isReadable(right)) {
            System.out.printf("file %s exists? %b ? ,isRegularFile %b ? ,isReadable %b ? \r\n", right, Files.exists(right),
                    Files.isRegularFile(right), Files.isReadable(right));
            return;

        }

        Properties leftProperties = new Properties();
        leftProperties.load(Files.newInputStream(left));


        Properties rightProperties = new Properties();
        rightProperties.load(Files.newInputStream(right));

        Map<String, CompareResult> compareResultMap = new HashMap<>();

        leftProperties.keySet().forEach(e -> {
            String value = leftProperties.getProperty((String) e);
            String rightValue = rightProperties.getProperty((String) e);
            if (rightValue == null) {
                compareResultMap.put((String) e, new CompareResult((String) e, DiffType.RIGHT_NOT_EXISTS, value, null));
            } else if (value.equals(rightValue)) {
                compareResultMap.put((String) e, new CompareResult((String) e, DiffType.NP_DIFF, value, rightValue));
            } else  if (!value.equals(rightValue)) {
                compareResultMap.put((String) e, new CompareResult((String) e, DiffType.VALUE_NOT_EQUAL, value, rightValue));
            }
        });

        rightProperties.keySet().forEach(e -> {
                    String key = (String) e;
                    if (!compareResultMap.containsKey(key)) {
                        String rightValue = rightProperties.getProperty(key);
                        compareResultMap.put(key, new CompareResult((String) e, DiffType.LEFT_NOT_EXISTS, null, rightValue));
                    }
                }
        );


        System.out.println("左侧文件路径:"+args[0]+"<br/>");
        System.out.println("右侧文件路径:"+args[1]+"<br/>");

        System.out.println("<table border='2px'>");
        System.out.println("<tr><td>key</td><td>diffType</td><td>leftValue</td><td>rightValue</td></tr>");
        String trLine = "<tr><td>key</td><td>diffType</td><td>leftValue</td><td>rightValue</td></tr>";

        compareResultMap.forEach( (k,v) ->{
            String line  = trLine;
            if(v.getDiffType() == DiffType.NP_DIFF){
                line  = line.replace("key",k).replace("diffType","无差异")
                        .replace("leftValue","").replace("rightValue","");

            } else if(v.getDiffType() == DiffType.VALUE_NOT_EQUAL){
                line  = line.replace("key",k).replace("diffType","<font color='red'>差异</font>")
                        .replace("leftValue",v.getLeftValue()).replace("rightValue",v.getRightValue());

            } else if(v.getDiffType() == DiffType.LEFT_NOT_EXISTS){
                line  = line.replace("key",k).replace("diffType","<font color='red'>左侧配置不存在</font>")
                        .replace("leftValue","").replace("rightValue",v.getRightValue());

            } else if(v.getDiffType() == DiffType.RIGHT_NOT_EXISTS){
                line  = line.replace("key",k).replace("diffType","<font color='red'>右侧配置不存在</font>")
                        .replace("leftValue",v.getLeftValue()).replace("rightValue","");


            }
            System.out.println(line);
        });
        System.out.println("</table>");


    }
}


class CompareResult {


    public CompareResult(String key, DiffType diffType, String leftValue, String rightValue) {
        this.key = key;
        this.diffType = diffType;
        this.leftValue = leftValue;
        this.rightValue = rightValue;
    }

    private String key;
    private DiffType diffType;
    private String leftValue;
    private String rightValue;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public DiffType getDiffType() {
        return diffType;
    }

    public void setDiffType(DiffType diffType) {
        this.diffType = diffType;
    }

    public String getLeftValue() {
        return leftValue;
    }

    public void setLeftValue(String leftValue) {
        this.leftValue = leftValue;
    }

    public String getRightValue() {
        return rightValue;
    }

    public void setRightValue(String rightValue) {
        this.rightValue = rightValue;
    }

    @Override
    public String toString() {
        return "CompareResult{" +
                "key='" + key + '\'' +
                ", diffType=" + diffType +
                ", leftValue='" + leftValue + '\'' +
                ", rightValue='" + rightValue + '\'' +
                '}';
    }
}

enum DiffType {
    NP_DIFF,
    LEFT_NOT_EXISTS,
    RIGHT_NOT_EXISTS,
    VALUE_NOT_EQUAL
}
