package org.wrj.dolphin.io;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class IOTest {
	
	public static void main(String[] args) {
		FileSystem fs = FileSystems.getDefault();
		Path path = fs.getPath(System.getenv("JAVA_HOME"));
		System.out.println(path.getRoot());
		System.out.println(path.getNameCount());
		System.out.println(path.toUri());
	}

}
