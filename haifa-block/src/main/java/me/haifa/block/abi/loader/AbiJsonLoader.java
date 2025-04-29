package me.haifa.block.abi.loader;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
public class AbiJsonLoader {

    private final ObjectMapper mapper = new ObjectMapper();

    public JsonNode loadAbi(String filePath) throws Exception {
        return mapper.readTree(new File(filePath));
    }
}
