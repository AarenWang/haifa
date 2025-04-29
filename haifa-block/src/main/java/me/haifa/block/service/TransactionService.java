package me.haifa.block.service;

import me.haifa.block.entity.TransactionEntity;
import me.haifa.block.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class TransactionService {

    @Resource
    private TransactionRepository txRepo;

    public void saveTransactionWithHooks(TransactionEntity tx) {
        // 🔧 预处理逻辑
        preprocess(tx);

        // 🗃️ 数据库保存
        txRepo.save(tx);

        // ✅ 后处理逻辑
        postprocess(tx);
    }

    private void preprocess(TransactionEntity tx) {
        // 例如：补充字段、清洗数据、校验等
        tx.setNormalizedInput(tx.getInput().toLowerCase());
    }

    private void postprocess(TransactionEntity tx) {
        // 例如：写日志、推送事件、发送 MQ
        System.out.println("✅ 写入交易: " + tx.getTxHash());
    }

    public void saveTransaction(TransactionEntity t) {
        txRepo.save(t);
    }
}
