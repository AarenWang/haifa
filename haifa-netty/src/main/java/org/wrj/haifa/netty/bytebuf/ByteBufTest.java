package org.wrj.haifa.netty.bytebuf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;

import java.nio.charset.StandardCharsets;

public class ByteBufTest {

    public static void main(String[] args) {
        System.out.println("=========================== begin =============================");
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(1024);
        byteBuf.writeBytes("Hello World".getBytes(StandardCharsets.UTF_8));
        ByteBufAllocator allocator = byteBuf.alloc();
        System.out.println("=========================== end =============================");

    }
}
