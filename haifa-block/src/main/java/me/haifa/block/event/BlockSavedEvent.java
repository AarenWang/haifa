package me.haifa.block.event;

import me.haifa.block.entity.BlockEntity;
import org.springframework.context.ApplicationEvent;

public class BlockSavedEvent extends ApplicationEvent {

    private final BlockEntity block;

    public BlockSavedEvent(Object source, BlockEntity block) {
        super(source);
        this.block = block;
    }

    public BlockEntity getBlock() {
        return block;
    }
}