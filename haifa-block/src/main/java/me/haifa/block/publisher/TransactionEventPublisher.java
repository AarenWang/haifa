package me.haifa.block.publisher;

import me.haifa.block.entity.TransactionEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventPublisher {

    @Autowired
    private ApplicationEventPublisher publisher;

    public void publishSavedTx(TransactionEntity tx) {
        publisher.publishEvent(new TransactionSavedEvent(this, tx));
    }
}



