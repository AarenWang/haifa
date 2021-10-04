package org.wrj.haifa.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

/**
 * Created by wangrenjun on 2017/4/23.
 */
public class SocketClient1 {

    static int i = 0;

    public static void main(String[] args) {
        for(int i = 0; i < 5; i++){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SocketClient1.client();
                }
            }).start();

        }

    }

    public static void client() {

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        SocketChannel socketChannel = null;
        try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(new InetSocketAddress("127.0.0.1", 10000));
            if (socketChannel.finishConnect()) {

                while (true) {
                    TimeUnit.SECONDS.sleep(1);
                    String info = "I'm " + i++ + "-th information from client,Client Thread name="+Thread.currentThread().getName();
                    buffer.clear();
                    buffer.put(info.getBytes());
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        System.out.println(buffer);// java.nio.HeapByteBuffer[pos=0 lim=33 cap=1024]
                        socketChannel.write(buffer);
                    }
                }

            }

        } catch (IOException | InterruptedException e) {
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
