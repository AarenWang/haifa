package org.wrj.haifa.netty.rsyslog.socketserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyslogNioServer {

    private static final int BUFFER_SIZE = 1024;
    private static final int PORT = 514;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);  // 用于处理解析任务的线程池
    private SyslogParser parser = new SyslogParser();

    public static void main(String[] args) throws IOException {
        new SyslogNioServer().start();
    }

    public void start() throws IOException {
        Selector selector = Selector.open();  // 打开Selector，用于处理多个通道

        // 配置 TCP Channel
        ServerSocketChannel tcpChannel = ServerSocketChannel.open();
        tcpChannel.bind(new InetSocketAddress(PORT));
        tcpChannel.configureBlocking(false);  // 设置为非阻塞
        tcpChannel.register(selector, SelectionKey.OP_ACCEPT);  // 注册接收事件

        // 配置 UDP Channel
        DatagramChannel udpChannel = DatagramChannel.open();
        udpChannel.bind(new InetSocketAddress(PORT));
        udpChannel.configureBlocking(false);  // 设置为非阻塞
        udpChannel.register(selector, SelectionKey.OP_READ);  // 注册读取事件

        System.out.println("Syslog NIO Server started on port " + PORT);

        while (true) {
            selector.select();  // 阻塞直到有通道准备好
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();  // 移除已处理的键

                if (key.isAcceptable()) {
                    handleTcpAccept(selector, key);  // 处理 TCP 连接接入
                } else if (key.isReadable()) {
                    if (key.channel() instanceof SocketChannel) {
                        handleTcpRead(key);  // 处理 TCP 读
                    } else if (key.channel() instanceof DatagramChannel) {
                        handleUdpRead(key);  // 处理 UDP 读
                    }
                }
            }
        }
    }

    // 处理 TCP 连接接入
    private void handleTcpAccept(Selector selector, SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverChannel.accept();
        socketChannel.configureBlocking(false);  // 设置非阻塞
        socketChannel.register(selector, SelectionKey.OP_READ);  // 注册读取事件
        System.out.println("Accepted new TCP connection from " + socketChannel.getRemoteAddress());
    }

    // 处理 TCP 数据读取
    private void handleTcpRead(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        try {
            int bytesRead = channel.read(buffer);
            if (bytesRead == -1) {
                channel.close();  // 读取结束时关闭通道
                return;
            }

            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            String syslogData = new String(data);

            // 使用线程池异步处理Syslog解析
            threadPool.submit(() -> {
                SyslogMessage message = parser.parse(syslogData);
                System.out.println("Received TCP Syslog: " + message);
            });

        } catch (IOException e) {
            e.printStackTrace();
            try {
                channel.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    // 处理 UDP 数据读取
    private void handleUdpRead(SelectionKey key) {
        DatagramChannel channel = (DatagramChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        try {
            buffer.clear();
            InetSocketAddress address = (InetSocketAddress) channel.receive(buffer);
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            String syslogData = new String(data);

            // 使用线程池异步处理Syslog解析
            threadPool.submit(() -> {
                SyslogMessage message = parser.parse(syslogData);
                System.out.println("Received UDP Syslog from " + address + ": " + message);
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

