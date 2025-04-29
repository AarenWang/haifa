package me.haifa.block.abi.factory;


import com.fasterxml.jackson.databind.JsonNode;
import me.haifa.block.abi.decoder.DynamicAbiEventDecoder;
import me.haifa.block.abi.parser.AbiEventParser;
import org.springframework.stereotype.Component;
import org.web3j.abi.datatypes.Event;

import java.util.*;

@Component
public class DecoderFactory {

    private final AbiEventParser parser = new AbiEventParser();

    public List<DynamicAbiEventDecoder> createDecoders(JsonNode abiJson) {
        List<DynamicAbiEventDecoder> decoders = new ArrayList<>();

        for (JsonNode node : abiJson) {
            if (node.has("type") && node.get("type").asText().equals("event")) {
                Event event = parser.parseEvent(node);
                decoders.add(new DynamicAbiEventDecoder(event));
            }
        }

        return decoders;
    }
}