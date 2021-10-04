package org.wrj.haifa.nio2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class LatestModified {

    public static void main(String[] args) throws IOException {
        File dir = new File("/data/logs/kano-service");
        Optional<Path> listPath = Files.list(dir.toPath())
                .sorted((p1, p2)-> Long.valueOf(p2.toFile().lastModified())
                        .compareTo(p1.toFile().lastModified()))
                .findFirst();

        System.out.println(listPath.get().toFile().getAbsolutePath());

    }
}
