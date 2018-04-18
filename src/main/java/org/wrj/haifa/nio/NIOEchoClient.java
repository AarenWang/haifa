package org.wrj.haifa.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Created by wangrenjun on 2017/4/24.
 */
public class NIOEchoClient {

    private static final String HOST    = System.getProperty("host", "127.0.0.1");

    private static final int    PORT    = Integer.parseInt(System.getProperty("port", "10000"));

    private static final String CONTENT = System.getProperty("content", "");

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.print("please set echo content");
        }
        client(CONTENT);

    }

    public static void client(String content) {


        ByteBuffer buffer = ByteBuffer.allocate(1024);
        SocketChannel socketChannel = null;
        try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(new InetSocketAddress(HOST, PORT));
            if (socketChannel.finishConnect()) {

                /**
                 * while (true) { TimeUnit.SECONDS.sleep(1); String info = "I'm " + i++ + "-th information from
                 * client,Client Thread name=" + Thread.currentThread().getName(); buffer.clear();
                 * buffer.put(info.getBytes()); buffer.flip(); while (buffer.hasRemaining()) {
                 * System.out.println(buffer);// java.nio.HeapByteBuffer[pos=0 lim=33 cap=1024]
                 * socketChannel.write(buffer); } }
                 */
                buffer.clear();
                buffer.put(content.getBytes());
                buffer.flip();
                while (buffer.hasRemaining()) {
                    socketChannel.write(buffer);
                }

            }

        } catch (IOException e) {
            e.printStackTrace();

        } finally {

            try {
                if (socketChannel != null) {
                    socketChannel.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

}
