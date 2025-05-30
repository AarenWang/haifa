package org.wrj.haifa.netty.rsyslog.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramChannel;
import org.wrj.haifa.netty.rsyslog.client.RFC5424Message;

import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for processing RFC 5424 messages received by the Rsyslog server
 */
public class RsyslogServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = Logger.getLogger(RsyslogServerHandler.class.getName());
    
    private RsyslogMessageProcessor messageProcessor;
    
    /**
     * Create a new RsyslogServerHandler
     * 
     * @param messageProcessor The processor for handling parsed messages
     */
    public RsyslogServerHandler(RsyslogMessageProcessor messageProcessor) {
        this.messageProcessor = messageProcessor != null ? messageProcessor : new DefaultRsyslogMessageProcessor();
    }
    
    /**
     * Create a new RsyslogServerHandler with a default message processor
     */
    public RsyslogServerHandler() {
        this(null);
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 判断是否是UDP通道
        if (ctx.channel() instanceof DatagramChannel) {
            logger.info("UDP channel active");
            return;
        }
        
        // TCP通道处理
        InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
        if (address != null) {
            logger.info("New client connected: " + address.getHostString() + ":" + address.getPort());
        } else {
            logger.info("New client connected with unknown address");
        }
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof RFC5424Message) {
            RFC5424Message syslogMessage = (RFC5424Message) msg;
            
            // Process the message
            try {
                messageProcessor.processMessage(syslogMessage, ctx);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error processing syslog message", e);
            }
        } else {
            logger.warning("Received unexpected message type: " + msg.getClass().getName());
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.log(Level.SEVERE, "Exception caught in RsyslogServerHandler", cause);
        ctx.close();
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // 判断是否是UDP通道
        if (ctx.channel() instanceof DatagramChannel) {
            logger.info("UDP channel inactive");
            return;
        }
        
        // TCP通道处理
        InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
        if (address != null) {
            logger.info("Client disconnected: " + address.getHostString() + ":" + address.getPort());
        } else {
            logger.info("Client disconnected with unknown address");
        }
    }
    
    /**
     * Interface for message processing logic
     */
    public interface RsyslogMessageProcessor {
        /**
         * Process a received Rsyslog message
         * 
         * @param message The received RFC5424Message
         * @param ctx The channel handler context
         */
        void processMessage(RFC5424Message message, ChannelHandlerContext ctx);
    }
    
    /**
     * Default implementation of message processor that logs messages
     */
    public static class DefaultRsyslogMessageProcessor implements RsyslogMessageProcessor {
        private static final Logger procLogger = Logger.getLogger(DefaultRsyslogMessageProcessor.class.getName());
        
        @Override
        public void processMessage(RFC5424Message message, ChannelHandlerContext ctx) {
            InetSocketAddress address = null;
            
            // 尝试获取远程地址，UDP模式下可能为空
            if (ctx.channel().remoteAddress() instanceof InetSocketAddress) {
                address = (InetSocketAddress) ctx.channel().remoteAddress();
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("Received syslog message");
            
            if (address != null) {
                sb.append(" from: ").append(address.getHostString());
            }
            
            sb.append("\n")
              .append("  Timestamp: ").append(message.getTimestamp()).append("\n")
              .append("  Hostname: ").append(message.getHostname()).append("\n")
              .append("  Application: ").append(message.getAppName()).append("\n")
              .append("  Process ID: ").append(message.getProcId()).append("\n")
              .append("  Message ID: ").append(message.getMsgId()).append("\n")
              .append("  Facility: ").append(message.getFacility()).append("\n")
              .append("  Severity: ").append(message.getSeverity()).append("\n")
              .append("  Structured Data: ").append(message.getStructuredData()).append("\n")
              .append("  Message: ").append(message.getMessage());
            
            // Log the formatted message
            procLogger.info(sb.toString());
        }
    }
} 