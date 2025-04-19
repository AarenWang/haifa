package org.wrj.haifa.netty.rsyslog.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.wrj.haifa.netty.rsyslog.client.RFC5424Message;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decoder for RFC 5424 Syslog messages
 * <p>
 * Format: <PRIVAL>VERSION TIMESTAMP HOSTNAME APP-NAME PROCID MSGID STRUCTURED-DATA MSG
 * </p>
 */
public class RsyslogDecoder extends ByteToMessageDecoder {
    private static final Logger logger = Logger.getLogger(RsyslogDecoder.class.getName());
    
    // Regex pattern for RFC 5424 syslog message format
    private static final Pattern RFC5424_PATTERN = Pattern.compile(
            "<(\\d+)>1 " +                            // PRI and VERSION
            "(\\S+) " +                               // TIMESTAMP
            "(\\S+) " +                               // HOSTNAME
            "(\\S+) " +                               // APP-NAME
            "(\\S+) " +                               // PROCID
            "(\\S+) " +                               // MSGID
            "(-|(?:\\[.*?\\])+)" +                    // STRUCTURED-DATA (- or [id name="value"...]
            "(?:\\s+(.+))?$");                         // MSG (optional)
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                    .withZone(ZoneId.systemDefault());

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 打印调试信息 - 接收到的字节数
        logger.fine("Decoding buffer with " + in.readableBytes() + " bytes");
        
        // Check if the ByteBuf has any readable bytes
        if (!in.isReadable()) {
            logger.fine("Buffer not readable, waiting for more data");
            return;
        }
        
        // 跳过开头的空白字符和换行符
        skipLeadingWhitespaceAndNewlines(in);
        
        // 如果跳过空白后缓冲区为空，则返回
        if (!in.isReadable()) {
            logger.fine("Buffer contains only whitespace/newlines, skipping");
            return;
        }
        
        // Mark the current reader index to reset if needed
        in.markReaderIndex();
        
        // 打印前100个字节的内容，帮助调试
        if (logger.isLoggable(Level.FINE)) {
            int length = Math.min(in.readableBytes(), 100);
            byte[] debugBytes = new byte[length];
            int readerIndex = in.readerIndex();
            in.getBytes(readerIndex, debugBytes);
            logger.fine("First " + length + " bytes: " + new String(debugBytes, StandardCharsets.UTF_8));
        }
        
        // 检测是否为UDP通道
        boolean isUdp = ctx.channel() instanceof io.netty.channel.socket.DatagramChannel;
        
        // 对于UDP通道，我们读取整个数据包
        if (isUdp) {
            int totalLength = in.readableBytes();
            if (totalLength > 0) {
                byte[] msgBytes = new byte[totalLength];
                in.readBytes(msgBytes);
                processMessage(msgBytes, out);
            }
            return;
        }
        
        // 以下是TCP模式处理 - 试图找到消息结束符
        int endIndex = findEndOfLine(in);
        if (endIndex == -1) {
            // Not a complete line yet, wait for more data
            logger.fine("No line ending found, waiting for more data");
            in.resetReaderIndex();
            return;
        }
        
        // Calculate the length of the message
        int length = endIndex - in.readerIndex();
        
        if (length <= 0) {
            logger.warning("Zero or negative message length calculated in TCP mode, skipping byte");
            // 消费掉可能的行结束符
            if (in.isReadable()) {
                in.skipBytes(1);
            }
            return;
        }
        
        // Read the message into a byte array
        byte[] msgBytes = new byte[length];
        in.readBytes(msgBytes);
        
        // Skip the line ending character(s)
        skipLineEndings(in);
        
        // 处理消息
        processMessage(msgBytes, out);
    }
    
    /**
     * 跳过缓冲区开头的空白字符和换行符
     */
    private void skipLeadingWhitespaceAndNewlines(ByteBuf buffer) {
        while (buffer.isReadable()) {
            byte b = buffer.getByte(buffer.readerIndex());
            if (b == ' ' || b == '\t' || b == '\n' || b == '\r') {
                buffer.skipBytes(1);
            } else {
                break;
            }
        }
    }
    
    /**
     * 跳过消息结尾的换行符
     */
    private void skipLineEndings(ByteBuf buffer) {
        while (buffer.isReadable()) {
            byte b = buffer.getByte(buffer.readerIndex());
            if (b == '\n' || b == '\r') {
                buffer.skipBytes(1);
            } else {
                break;
            }
        }
    }
    
    /**
     * 处理解析出的消息字节
     */
    private void processMessage(byte[] msgBytes, List<Object> out) {
        // Convert the byte array to a string
        String msgStr = new String(msgBytes, StandardCharsets.UTF_8).trim();
        
        if (msgStr.isEmpty()) {
            logger.fine("Empty message after trimming, ignoring");
            return;
        }
        
        logger.fine("Extracted message: " + msgStr);
        
        try {
            // Parse the RFC 5424 message
            RFC5424Message message = parseRFC5424Message(msgStr);
            if (message != null) {
                out.add(message);
                logger.info("Decoded RFC 5424 message: " + message.getMessage());
            } else {
                logger.warning("Failed to parse message: " + msgStr);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to decode message: " + msgStr, e);
        }
    }
    
    /**
     * Find the end of a line in the ByteBuf
     * 
     * @param buffer The ByteBuf to search
     * @return The index of the end of the line, or -1 if not found
     */
    private int findEndOfLine(ByteBuf buffer) {
        int startIndex = buffer.readerIndex();
        int endIndex = buffer.writerIndex();
        
        for (int i = startIndex; i < endIndex; i++) {
            byte b = buffer.getByte(i);
            if (b == '\n' || b == '\r') {
                return i;
            }
        }
        
        return -1;
    }
    
    /**
     * Parse an RFC 5424 message string into an RFC5424Message object
     * 
     * @param msgStr The message string to parse
     * @return A parsed RFC5424Message object, or null if parsing failed
     */
    private RFC5424Message parseRFC5424Message(String msgStr) {
        Matcher matcher = RFC5424_PATTERN.matcher(msgStr);
        
        if (!matcher.matches()) {
            logger.warning("Message does not match RFC 5424 format: " + msgStr);
            return null;
        }
        
        try {
            int priVal = Integer.parseInt(matcher.group(1));
            int facility = priVal / 8;
            int severity = priVal % 8;
            
            RFC5424Message message = new RFC5424Message()
                    .setFacility(facility)
                    .setSeverity(severity)
                    .setTimestamp(matcher.group(2))
                    .setHostname(matcher.group(3))
                    .setAppName(matcher.group(4))
                    .setProcId(matcher.group(5))
                    .setMsgId(matcher.group(6))
                    .setStructuredData(matcher.group(7));
            
            // MSG part is optional
            if (matcher.groupCount() >= 8 && matcher.group(8) != null) {
                message.setMessage(matcher.group(8));
            }
            
            return message;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error parsing RFC 5424 message: " + msgStr, e);
            return null;
        }
    }
} 