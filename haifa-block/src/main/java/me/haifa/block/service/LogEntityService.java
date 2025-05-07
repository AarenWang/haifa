package me.haifa.block.service;

import me.haifa.block.entity.LogEntity;
import me.haifa.block.publisher.LogEventPublisher;
import me.haifa.block.repository.LogEntityRepository;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.Log;

import javax.annotation.Resource;
import java.util.List;

@Service
public class LogEntityService {

    @Resource
    LogEventPublisher logEventPublisher;

    @Resource
    private LogEntityRepository logRepo;


    public void saveLog(LogEntity logEntity) {
        logRepo.save(logEntity);
    }

    public void saveWeb3jLog(Log l,String txHash) {
        LogEntity logEntity = new LogEntity();
        logEntity.setTxHash(txHash);
        //logEntity.setLogIndex(i);
        logEntity.setAddress(l.getAddress());
        List<String> topics = l.getTopics();
        if (topics.size() > 0) logEntity.setTopic0(topics.get(0));
        if (topics.size() > 1) logEntity.setTopic1(topics.get(1));
        if (topics.size() > 2) logEntity.setTopic2(topics.get(2));
        if (topics.size() > 3) logEntity.setTopic3(topics.get(3));
        logEntity.setData(l.getData());
        this.saveLog(logEntity);

        logEventPublisher.publish(logEntity,l);

    }
}
