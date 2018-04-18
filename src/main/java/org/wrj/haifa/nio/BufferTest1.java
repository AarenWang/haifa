package org.wrj.haifa.nio;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by wangrenjun on 2017/4/22.
 */
public class BufferTest1 {

    public static void main(String[] args) throws Exception {

        String path = BufferTest1.class.getResource("/data/nio-data.txt").getPath();
        RandomAccessFile aFile = new RandomAccessFile(path, "rw");
        FileChannel inChannel = aFile.getChannel();

        // create buffer with capacity of 48 bytes
        ByteBuffer buf = ByteBuffer.allocate(48);

        int bytesRead = inChannel.read(buf); // read into buffer.
        while (bytesRead != -1) {

            buf.flip(); // make buffer ready for read

            while (buf.hasRemaining()) {
                System.out.print((char) buf.get()); // read 1 byte at a time
            }

            buf.clear(); // make buffer ready for writing
            bytesRead = inChannel.read(buf);
        }

        inChannel.write(buf);

        aFile.close();

    }
}
