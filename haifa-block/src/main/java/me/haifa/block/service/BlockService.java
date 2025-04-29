package me.haifa.block.service;


import me.haifa.block.entity.BlockEntity;
import me.haifa.block.repository.BlockRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class BlockService {

    @Resource
    private  BlockRepository blockRepo;


    @Async
    public void saveBlock(BlockEntity b) {
        blockRepo.save(b);
    }
}