package org.wrj.haifa.netty.rsyslog.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.wrj.haifa.netty.rsyslog.client.RFC5424Message;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
        // Check if the ByteBuf has any readable bytes
        if (!in.isReadable()) {
            return;
        }
        
        // Mark the current reader index to reset if needed
        in.markReaderIndex();
        
        // Try to find a line ending (messages typically end with \n)
        int endIndex = findEndOfLine(in);
        if (endIndex == -1) {
            // Not a complete line yet, wait for more data
            in.resetReaderIndex();
            return;
        }
        
        // Calculate the length of the message
        int length = endIndex - in.readerIndex();
        
        // Read the message into a byte array
        byte[] msgBytes = new byte[length];
        in.readBytes(msgBytes);
        
        // Skip the line ending character(s)
        if (in.readableBytes() > 0) {
            byte b = in.readByte();
            // If we have CR+LF, skip the LF as well
            if (b == '\r' && in.readableBytes() > 0 && in.getByte(in.readerIndex()) == '\n') {
                in.readByte();
            }
        }
        
        // Convert the byte array to a string
        String msgStr = new String(msgBytes, StandardCharsets.UTF_8);
        
        try {
            // Parse the RFC 5424 message
            RFC5424Message message = parseRFC5424Message(msgStr);
            if (message != null) {
                out.add(message);
                logger.info("Decoded RFC 5424 message: " + message.getMessage());
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
        int i = buffer.forEachByte(b -> b != '\n' && b != '\r');
        return (i == -1) ? buffer.writerIndex() : i;
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