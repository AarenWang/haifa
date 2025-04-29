package me.haifa.block.event;

import me.haifa.block.entity.LogEntity;
import org.springframework.context.ApplicationEvent;
import org.web3j.protocol.core.methods.response.Log;

public class LogSavedEvent extends ApplicationEvent {
    private final LogEntity log;

    private final Log  web3jLog;



    public LogSavedEvent(Object source,Log web3jLog, LogEntity log) {
        super(source);
        this.log = log;
        this.web3jLog = web3jLog;
    }

    public LogEntity getLog() {
        return log;
    }

    public Log getWeb3jLog() {
        return web3jLog;
    }
}
