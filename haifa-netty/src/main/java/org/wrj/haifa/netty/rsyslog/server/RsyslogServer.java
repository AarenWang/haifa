package org.wrj.haifa.netty.rsyslog.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Rsyslog server implementation that supports RFC 5424 protocol
 */
public class RsyslogServer {
    private static final Logger logger = Logger.getLogger(RsyslogServer.class.getName());
    
    private final int port;
    private final RsyslogServerHandler.RsyslogMessageProcessor messageProcessor;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;
    
    /**
     * Create a new Rsyslog server
     * 
     * @param port The port to listen on
     */
    public RsyslogServer(int port) {
        this(port, null);
    }
    
    /**
     * Create a new Rsyslog server with a custom message processor
     * 
     * @param port The port to listen on
     * @param messageProcessor The processor for handling parsed messages
     */
    public RsyslogServer(int port, RsyslogServerHandler.RsyslogMessageProcessor messageProcessor) {
        this.port = port;
        this.messageProcessor = messageProcessor;
    }
    
    /**
     * Start the Rsyslog server
     * 
     * @throws Exception If an error occurs while starting the server
     */
    public void start() throws Exception {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(
                                    new RsyslogDecoder(),
                                    new RsyslogServerHandler(messageProcessor)
                            );
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            
            // Bind and start to accept incoming connections
            channelFuture = bootstrap.bind(port).sync();
            logger.info("Rsyslog server started on port " + port);
            
            // Wait until the server socket is closed
            channelFuture.channel().closeFuture().sync();
        } finally {
            shutdown();
        }
    }
    
    /**
     * Shutdown the Rsyslog server
     */
    public void shutdown() {
        logger.info("Shutting down Rsyslog server");
        
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }
        
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
    }
    
    /**
     * Create a custom message processor implementation
     */
    public static class CustomMessageProcessor implements RsyslogServerHandler.RsyslogMessageProcessor {
        private static final Logger customLogger = Logger.getLogger(CustomMessageProcessor.class.getName());
        
        @Override
        public void processMessage(org.wrj.haifa.netty.rsyslog.client.RFC5424Message message, io.netty.channel.ChannelHandlerContext ctx) {
            // 在这里可以实现自定义的消息处理逻辑
            // 例如将消息存储到数据库、发送到消息队列、触发告警等
            
            // 示例: 根据消息的严重性级别进行不同的处理
            int severity = message.getSeverity();
            String logMessage = String.format("[%s] %s: %s", 
                    message.getHostname(), 
                    message.getAppName(), 
                    message.getMessage());
            
            switch (severity) {
                case org.wrj.haifa.netty.rsyslog.client.RFC5424Message.SEVERITY_EMERGENCY:
                case org.wrj.haifa.netty.rsyslog.client.RFC5424Message.SEVERITY_ALERT:
                case org.wrj.haifa.netty.rsyslog.client.RFC5424Message.SEVERITY_CRITICAL:
                    customLogger.severe(logMessage);
                    // 可以在这里添加告警逻辑
                    break;
                    
                case org.wrj.haifa.netty.rsyslog.client.RFC5424Message.SEVERITY_ERROR:
                    customLogger.severe(logMessage);
                    break;
                    
                case org.wrj.haifa.netty.rsyslog.client.RFC5424Message.SEVERITY_WARNING:
                    customLogger.warning(logMessage);
                    break;
                    
                case org.wrj.haifa.netty.rsyslog.client.RFC5424Message.SEVERITY_NOTICE:
                case org.wrj.haifa.netty.rsyslog.client.RFC5424Message.SEVERITY_INFORMATIONAL:
                    customLogger.info(logMessage);
                    break;
                    
                case org.wrj.haifa.netty.rsyslog.client.RFC5424Message.SEVERITY_DEBUG:
                    customLogger.fine(logMessage);
                    break;
            }
        }
    }
    
    public static void main(String[] args) {
        // Default port is 514, which is the standard syslog port
        int port = 514;
        
        // Allow port to be specified as a command-line argument
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Invalid port number, using default: " + port, e);
            }
        }
        
        logger.info("Starting Rsyslog server on port " + port);
        
        // Create a custom message processor (optional)
        CustomMessageProcessor customProcessor = new CustomMessageProcessor();
        
        // Create and start the server
        RsyslogServer server = new RsyslogServer(port, customProcessor);
        
        try {
            server.start();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to start Rsyslog server", e);
        }
    }
}
