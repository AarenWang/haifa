package me.haifa.block.service;

import me.haifa.block.entity.LogEntity;
import me.haifa.block.repository.LogEntityRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class LogEntityService {

    @Resource
    private LogEntityRepository logRepo;


    public void saveLog(LogEntity logEntity) {
        logRepo.save(logEntity);
    }
}
