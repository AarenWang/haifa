package org.wrj.haifa.netty.rsyslog.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;
import java.util.logging.Logger;

/**
 * 专门用于处理UDP数据包的Rsyslog解码器
 */
public class RsyslogDatagramDecoder extends MessageToMessageDecoder<DatagramPacket> {
    private static final Logger logger = Logger.getLogger(RsyslogDatagramDecoder.class.getName());
    private final RsyslogDecoder decoder = new RsyslogDecoder();
    
    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        logger.info("Received UDP datagram from: " + msg.sender().getHostString());
        ByteBuf content = msg.content();
        
        // 使用已有的ByteBuf解码器处理内容
        decoder.decode(ctx, content, out);
    }
} 