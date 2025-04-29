package me.haifa.block.listener;

import me.haifa.block.dispatcher.LogEventDispatcher;
import me.haifa.block.entity.LogEntity;
import me.haifa.block.event.LogSavedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class LogEventListener {

    private final LogEventDispatcher logDispatcher;

    public LogEventListener(LogEventDispatcher logDispatcher) {
        this.logDispatcher = logDispatcher;
    }

    @Async
    @EventListener
    public void onLogSaved(LogSavedEvent event) {
        LogEntity log = event.getLog();
        System.out.println("📜 日志保存后处理，事件签名：" + log.getTopic0());

        // 分发给对应解码器处理结构化数据（如 Transfer/Approval）
        logDispatcher.dispatch(event.getWeb3jLog(),event.getLog());
    }
}