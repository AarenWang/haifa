package me.haifa.block.publisher;

import me.haifa.block.entity.TransactionEntity;
import me.haifa.block.event.TransactionSavedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class TransactionEventPublisher {

    @Resource
    private ApplicationEventPublisher publisher;

    public void publishSavedTx(TransactionEntity tx) {
        publisher.publishEvent(new TransactionSavedEvent(this, tx));
    }
}



