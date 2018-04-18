package org.wrj.haifa.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Created by wangrenjun on 2017/4/24.
 */
public class NIOEchoServer {

    private static final String HOST     = System.getProperty("host", "127.0.0.1");

    private static final int    PORT     = Integer.parseInt(System.getProperty("port", "10000"));

    private static final int    BUF_SIZE = 1024;

    private static final int    TIMEOUT  = 3000;

    public static void main(String[] args) {
        selector();
    }

    public static void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel ssChannel = (ServerSocketChannel) key.channel();
        SocketChannel sc = ssChannel.accept();
        sc.configureBlocking(false);
        sc.register(key.selector(), SelectionKey.OP_READ | SelectionKey.OP_WRITE, ByteBuffer.allocateDirect(BUF_SIZE));
    }

    public static void handleRead(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();
        InetSocketAddress sa = (InetSocketAddress) sc.getRemoteAddress();
        ByteBuffer buf = (ByteBuffer) key.attachment();
        long bytesRead = sc.read(buf);
        StringBuilder sb = new StringBuilder();
        while (bytesRead > 0) {
            buf.flip();
            while (buf.hasRemaining()) {
                char c = (char) buf.get();
                // System.out.print(c);
                sb.append(c);
            }
            // System.out.println();
            String echo = "Read from " + sa.getHostName() + ":" + sa.getPort() + ",echo content=[" + sb+"]";
            System.out.println(echo);
            // sc.write(buf);
            buf.clear();
            bytesRead = sc.read(buf);
        }
        ///if (bytesRead == -1) {
        //    sc.close();
        //}

        //读到内容写回去
        //buf.flip();
        buf.put(sb.toString().length() ==0 ? "--".getBytes() : sb.toString().getBytes());
    }

    public static void handleWrite(SelectionKey key) throws IOException {
        ByteBuffer buf = (ByteBuffer) key.attachment();
        buf.flip();
        SocketChannel sc = (SocketChannel) key.channel();
        while (buf.hasRemaining()) {
            sc.write(buf);
        }
        buf.compact();//为啥要调用compack()方法

        //一次处理完就关闭链接
        key.cancel();
        sc.close();
        System.out.println("close client connection!!!");

    }

    public static void selector() {
        Selector selector = null;
        ServerSocketChannel ssc = null;
        try {
            selector = Selector.open();
            ssc = ServerSocketChannel.open();
            ssc.socket().bind(new InetSocketAddress(PORT));
            ssc.configureBlocking(false);
            ssc.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("NIOEchoServer start successfull........");
            while (true) {
                if (selector.select(TIMEOUT) == 0) {
                    //System.out.println("==");
                    continue;
                }
                Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    if (key.isValid() && key.isAcceptable()) {
                        handleAccept(key);
                    }
                    if (key.isValid() && key.isReadable()) {
                        handleRead(key);
                    }
                    if (key.isValid() && key.isWritable()) {
                        handleWrite(key);
                    }
                    if (key.isValid() && key.isConnectable()) {
                        System.out.println("isConnectable = true");
                    }
                    iter.remove();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (selector != null) {
                    selector.close();
                }
                if (ssc != null) {
                    ssc.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
