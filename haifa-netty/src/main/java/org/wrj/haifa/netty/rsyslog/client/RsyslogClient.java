package org.wrj.haifa.netty.rsyslog.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Rsyslog client implementation that supports RFC 5424 protocol
 */
public class RsyslogClient {
    private static final Logger logger = Logger.getLogger(RsyslogClient.class.getName());
    
    private final String host;
    private final int port;
    private Channel channel;
    private EventLoopGroup group;
    private boolean connected = false;
    
    /**
     * Creates a new Rsyslog client instance
     * 
     * @param host The hostname or IP address of the Rsyslog server
     * @param port The port number of the Rsyslog server
     */
    public RsyslogClient(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    /**
     * Connects to the Rsyslog server
     * 
     * @param timeout The timeout for connection in seconds
     * @return true if connection was successful, false otherwise
     */
    public boolean connect(int timeout) {
        if (connected) {
            return true;
        }
        
        group = new NioEventLoopGroup();
        Promise<Void> connectPromise = new DefaultPromise<>(group.next());
        
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress(host, port))
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) TimeUnit.SECONDS.toMillis(timeout))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new RsyslogEncoder(),
                                    new RsyslogClientHandler(connectPromise)
                            );
                        }
                    });
            
            // Connect to the server
            ChannelFuture future = bootstrap.connect();
            channel = future.channel();
            
            // Wait for the connection to be established or timeout
            connectPromise.await(timeout, TimeUnit.SECONDS);
            
            if (connectPromise.isSuccess()) {
                connected = true;
                return true;
            } else {
                logger.log(Level.SEVERE, "Failed to connect to Rsyslog server: " + 
                          connectPromise.cause().getMessage());
                disconnect();
                return false;
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to Rsyslog server", e);
            disconnect();
            return false;
        }
    }
    
    /**
     * Disconnects from the Rsyslog server
     */
    public void disconnect() {
        if (channel != null) {
            channel.close();
            channel = null;
        }
        
        if (group != null) {
            group.shutdownGracefully();
            group = null;
        }
        
        connected = false;
    }
    
    /**
     * Sends a log message to the Rsyslog server
     * 
     * @param message The RFC5424Message to send
     * @return true if the message was successfully sent, false otherwise
     */
    public boolean sendMessage(RFC5424Message message) {
        if (!connected || channel == null) {
            logger.severe("Not connected to Rsyslog server");
            return false;
        }
        
        try {
            channel.writeAndFlush(message).sync();
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error sending message to Rsyslog server", e);
            return false;
        }
    }
    
    /**
     * Sends a simple text message with default settings to the Rsyslog server
     * 
     * @param text The message text to send
     * @param severity The severity level (use RFC5424Message.SEVERITY_* constants)
     * @param facility The facility (use RFC5424Message.FACILITY_* constants)
     * @return true if the message was successfully sent, false otherwise
     */
    public boolean sendMessage(String text, int severity, int facility) {
        RFC5424Message message = new RFC5424Message()
                .setSeverity(severity)
                .setFacility(facility)
                .setMessage(text);
        
        return sendMessage(message);
    }
    
    /**
     * Checks if the client is connected to the Rsyslog server
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return connected && channel != null && channel.isActive();
    }
    
    public static void main(String[] args) {
        // Example usage
        String host = "localhost";
        int port = 514; // Default syslog port
        
        if (args.length >= 2) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        }
        
        RsyslogClient client = new RsyslogClient(host, port);
        
        try {
            // Connect to the server with a 5 second timeout
            if (client.connect(5)) {
                logger.info("Connected to Rsyslog server " + host + ":" + port);
                
                // Send a test message
                client.sendMessage("Test message from Rsyslog client", 
                        RFC5424Message.SEVERITY_INFORMATIONAL, 
                        RFC5424Message.FACILITY_USER);
                
                // Send a more customized message
                RFC5424Message customMessage = new RFC5424Message()
                        .setSeverity(RFC5424Message.SEVERITY_NOTICE)
                        .setFacility(RFC5424Message.FACILITY_LOCAL0)
                        .setAppName("RsyslogClient")
                        .setMsgId("TEST123")
                        .setMessage("This is a custom message from the Rsyslog client");
                
                client.sendMessage(customMessage);
                
                // Wait a moment to ensure messages are sent
                Thread.sleep(1000);
            } else {
                logger.severe("Failed to connect to Rsyslog server");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in Rsyslog client", e);
        } finally {
            client.disconnect();
        }
    }
}
