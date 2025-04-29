package me.haifa.block.service;

import me.haifa.block.entity.LogExtEntity;
import me.haifa.block.repository.LogExtRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class LogExtService {

    @Resource
    private LogExtRepository repository;

    public void save(LogExtEntity ext) {
        repository.save(ext);
    }


}
