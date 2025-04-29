package me.haifa.block.decoder;

import me.haifa.block.entity.LogEntity;
import org.web3j.protocol.core.methods.response.Log;

public interface LogEventDecoder {
    /** 事件对应的 topic0 签名哈希（用于匹配） */
    String getSupportedEventSignature();

    /** 解码并保存 LogExtEntity（可含事务） */
    void decodeAndPersist(Log log, LogEntity savedLog);
}