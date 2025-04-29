package me.haifa.block.listener;

import me.haifa.block.entity.BlockEntity;
import me.haifa.block.event.BlockSavedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class BlockEventListener {

    @Async
    @EventListener
    public void onBlockSaved(BlockSavedEvent event) {
        BlockEntity block = event.getBlock();
        System.out.println("🧱 区块保存后处理：高度 " + block.getBlockNumber());
        // TODO: 异步计算、记录指标、存入缓存等
    }
}
