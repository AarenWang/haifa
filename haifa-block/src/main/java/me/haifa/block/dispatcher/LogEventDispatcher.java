package me.haifa.block.dispatcher;

import me.haifa.block.decoder.LogEventDecoder;
import me.haifa.block.entity.LogEntity;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.methods.response.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component

public class LogEventDispatcher {

    private final Map<String, LogEventDecoder> decoderMap = new HashMap<>();

    public LogEventDispatcher(List<LogEventDecoder> decoders) {
        for (LogEventDecoder decoder : decoders) {
            decoderMap.put(decoder.getSupportedEventSignature(), decoder);
        }
    }

    public void dispatch(Log log, LogEntity savedLog) {
        String topic0 = log.getTopics().get(0);
        LogEventDecoder decoder = decoderMap.get(topic0);

        if (decoder != null) {
            decoder.decodeAndPersist(log, savedLog);
        } else {
            System.out.println("⚠️ 未找到匹配的解码器: " + topic0);
        }
    }

}
