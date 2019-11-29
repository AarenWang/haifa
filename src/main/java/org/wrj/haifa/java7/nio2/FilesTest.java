package org.wrj.haifa.java7.nio2;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FilesTest {

    @Test
    public void test1() throws IOException {

        Path path = Paths.get(System.getenv("HOME")+"/ext_js/","1.txt");
        Files.write(path,"1".getBytes());
        Files.write(path,"2".getBytes());

    }
}
