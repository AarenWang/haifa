package me.haifa.block.publisher;

import me.haifa.block.entity.LogEntity;
import me.haifa.block.event.LogSavedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.methods.response.Log;

@Component
public class LogEventPublisher {


    private final ApplicationEventPublisher publisher;

    public LogEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(LogEntity log, Log web3jLog) {
        publisher.publishEvent(new LogSavedEvent(this,web3jLog, log));
    }
}