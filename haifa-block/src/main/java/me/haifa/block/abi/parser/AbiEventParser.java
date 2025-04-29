package me.haifa.block.abi.parser;


import com.fasterxml.jackson.databind.JsonNode;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;

import java.util.*;

public class AbiEventParser {

    public Event parseEvent(JsonNode abiNode) {
        String name = abiNode.get("name").asText();
        List<TypeReference<?>> inputs = new ArrayList<>();

        for (JsonNode input : abiNode.get("inputs")) {
            boolean indexed = input.get("indexed").asBoolean(false);
            String type = input.get("type").asText();

            TypeReference<?> ref = switch (type) {
                case "address" -> new TypeReference<Address>(indexed) {};
                case "uint256" -> new TypeReference<Uint256>(indexed) {};
                default -> throw new IllegalArgumentException("Unsupported type: " + type);
            };

            inputs.add((TypeReference<Type>) ref);
        }

        return new Event(name, inputs);
    }
}
