package org.wrj.haifa.netty.rsyslog.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Encoder for Rsyslog messages using RFC 5424 format
 */
public class RsyslogEncoder extends MessageToByteEncoder<RFC5424Message> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RFC5424Message msg, ByteBuf out) throws Exception {
        // Convert the message to RFC 5424 format
        byte[] data = msg.format();
        
        // Write the data to the ByteBuf
        out.writeBytes(data);
    }
} 