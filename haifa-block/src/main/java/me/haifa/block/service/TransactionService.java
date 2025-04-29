package me.haifa.block.service;

import me.haifa.block.entity.TransactionEntity;
import me.haifa.block.publisher.TransactionEventPublisher;
import me.haifa.block.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class TransactionService {

    @Resource
    private TransactionRepository txRepo;

    @Resource
    private TransactionEventPublisher txEventPublisher;

    public void saveTransactionWithHooks(TransactionEntity tx) {
        // 🔧 预处理逻辑
        preprocess(tx);

        // 🗃️ 数据库保存
        txRepo.save(tx);

        // ✅ 后处理逻辑
        txEventPublisher.publishSavedTx(tx);
    }

    private void preprocess(TransactionEntity tx) {
        // 例如：补充字段、清洗数据、校验等
        tx.setNormalizedInput(tx.getInput().toLowerCase());
    }


    public void saveTransaction(TransactionEntity t) {
        txRepo.save(t);
    }
}
