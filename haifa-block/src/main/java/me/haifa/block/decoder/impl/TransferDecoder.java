package me.haifa.block.decoder.impl;

import me.haifa.block.decoder.LogEventDecoder;
import me.haifa.block.entity.LogEntity;
import me.haifa.block.entity.LogExtEntity;
import me.haifa.block.service.LogExtService;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.crypto.Hash;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

@Component
public class TransferDecoder implements LogEventDecoder {

    @Resource
    private  LogExtService logExtService;

    public TransferDecoder(LogExtService logExtService) {
        this.logExtService = logExtService;
    }

    @Override
    public String getSupportedEventSignature() {
        return Hash.sha3String("Transfer(address,address,uint256)");
    }


    @Override
    public void decodeAndPersist(Log log, LogEntity savedLog) {
        List<String> topics = log.getTopics();
        if (topics.size() < 3) return;

        String from = "0x" + topics.get(1).substring(26);
        String to = "0x" + topics.get(2).substring(26);

        List decoded = FunctionReturnDecoder.decode(log.getData(), Arrays.asList(new TypeReference<Uint256>() {}.getSubTypeReference()));
        BigInteger value = (BigInteger) decoded.get(2);

        LogExtEntity ext = new LogExtEntity();
        ext.setLogId(savedLog.getId());
        ext.setEventType("Transfer");
        ext.setFromAddress(from);
        ext.setToAddress(to);
        ext.setAmount(value);
        ext.setTokenAddress(log.getAddress());

        logExtService.save(ext);
    }
}