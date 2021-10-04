package org.wrj.haifa.io;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

public class FilenameFilterDemo {
    public static void main(String[] args) {

        FilenameFilter filenameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                //System.out.println(dir.getName()+":"+name);
                return  name.matches("\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}");
                // return name.startsWith();
            }
        };


        FilenameFilter alTxt = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
               // System.out.println(dir.getName()+":"+name);
                return name.endsWith(".txt");
            }
        };

        String home = System.getenv("HOME");
        System.out.println("HOME is :"+home);
        File file = new File(home+"/temp_ip/");
        System.out.print(file.getAbsolutePath());


        File[] filenames = file.listFiles(filenameFilter);
        Arrays.stream(filenames).forEach(e -> System.out.println(e.getAbsolutePath()));

        Arrays.stream(filenames).forEach(e -> System.out.println(e.getName().substring(1)));

    }
}
