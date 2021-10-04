package org.wrj.dolphin.io;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class PathTest {

	public static void main(String[] args) {
		Path path = FileSystems.getDefault().getPath("F://", "java","docs","index.html");
		System.out.println(path);
		System.out.println(path.getName(0)+":"+path.getName(1));
		Path toPath =  FileSystems.getDefault().getPath("F://", "index.html");
		try {
			//Files.copy(path, toPath, StandardCopyOption.COPY_ATTRIBUTES);
			//Path link = FileSystems.getDefault().getPath("F://", "java","docs","index_link.html");
			//Path symbolicLink = FileSystems.getDefault().getPath("F://", "java","docs","index_symbolicLink.html");
			//Files.createLink(link, path);
			//FileAttribute<T>  fa = PosixFilePermissions.asFileAttribute(arg0);
			//Files.createSymbolicLink(path, symbolicLink,  null);
			FileStore fs = Files.getFileStore(path);
			System.out.println("totalspace="+fs.getTotalSpace() +",unallocatespace="+  fs.getUnallocatedSpace()+",usablespace="+fs.getUsableSpace());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
