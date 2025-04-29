package me.haifa.block.abi.decoder;


import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Event;
import org.web3j.protocol.core.methods.response.Log;

import java.util.List;

public class DynamicAbiEventDecoder {

    private final Event event;

    public DynamicAbiEventDecoder(Event event) {
        this.event = event;
    }

    public List<org.web3j.abi.datatypes.Type> decode(Log log) {
        return FunctionReturnDecoder.decode(log.getData(), event.getNonIndexedParameters());
    }

    public String getEventSignature() {
        //return org.web3j.crypto.Hash.sha3(event.getSignature());
        return EventEncoder.encode(this.event); // 返回 topic0
    }
}
