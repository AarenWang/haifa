package me.haifa.block.listener;


import me.haifa.block.entity.TransactionEntity;
import me.haifa.block.event.TransactionSavedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventListener {

    @EventListener
    public void handleTx(TransactionSavedEvent event) {
        TransactionEntity tx = event.getTx();
        // 异步日志、告警通知等
        System.out.println("📣 异步处理交易: " + tx.getTxHash());
    }
}