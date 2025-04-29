package me.haifa.block.publisher;

import me.haifa.block.entity.BlockEntity;
import me.haifa.block.event.BlockSavedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class BlockEventPublisher {

    private final ApplicationEventPublisher publisher;

    public BlockEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(BlockEntity block) {
        publisher.publishEvent(new BlockSavedEvent(this, block));
    }
}