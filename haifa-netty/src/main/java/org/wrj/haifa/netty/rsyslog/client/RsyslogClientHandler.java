package org.wrj.haifa.netty.rsyslog.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for the Rsyslog client's channel events
 */
public class RsyslogClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = Logger.getLogger(RsyslogClientHandler.class.getName());
    
    private final Promise<Void> connectPromise;
    
    public RsyslogClientHandler(Promise<Void> connectPromise) {
        this.connectPromise = connectPromise;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.info("Connected to the Rsyslog server");
        connectPromise.setSuccess(null);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.log(Level.SEVERE, "Exception caught in RsyslogClientHandler", cause);
        if (!connectPromise.isDone()) {
            connectPromise.setFailure(cause);
        }
        ctx.close();
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("Disconnected from the Rsyslog server");
    }
} 