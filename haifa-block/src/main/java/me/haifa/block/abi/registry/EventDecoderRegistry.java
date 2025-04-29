package me.haifa.block.abi.registry;


import me.haifa.block.abi.decoder.DynamicAbiEventDecoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class EventDecoderRegistry {

    private final Map<String, DynamicAbiEventDecoder> decoderMap = new HashMap<>();

    public void register(DynamicAbiEventDecoder decoder) {
        decoderMap.put(decoder.getEventSignature(), decoder);
    }

    public DynamicAbiEventDecoder getByTopic0(String topic0) {
        return decoderMap.get(topic0);
    }
}
