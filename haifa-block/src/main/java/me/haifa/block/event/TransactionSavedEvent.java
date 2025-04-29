package me.haifa.block.event;

import me.haifa.block.entity.TransactionEntity;
import org.springframework.context.ApplicationEvent;

public class TransactionSavedEvent extends ApplicationEvent {
    private final TransactionEntity tx;

    public TransactionSavedEvent(Object source, TransactionEntity tx) {
        super(source);
        this.tx = tx;
    }

    public TransactionEntity getTx() {
        return tx;
    }
}