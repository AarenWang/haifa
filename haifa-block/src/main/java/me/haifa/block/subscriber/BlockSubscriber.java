package me.haifa.block.subscriber;


import me.haifa.block.entity.BlockEntity;
import me.haifa.block.entity.LogEntity;
import me.haifa.block.entity.TransactionEntity;
import me.haifa.block.service.BlockService;
import me.haifa.block.service.LogEntityService;
import me.haifa.block.service.TransactionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.websocket.WebSocketService;

import okhttp3.OkHttpClient;

import java.net.InetSocketAddress;
import java.net.Proxy;


import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;

@Component
public class BlockSubscriber {

    @Resource
    private  BlockService blockService;

    @Resource
    private  TransactionService txService;

    @Resource
    private LogEntityService logService;

    @Value("${web3j.ws.url}")
    private String web3jUrl;


    @PostConstruct
    public void start() throws Exception {
        //WebSocketService webSocketService = new WebSocketService(web3jUrl, true);

        String proxyHost = "127.0.0.1";
        int proxyPort = 1081;

        // 设置 HTTP 代理
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));

        // 创建 OkHttpClient 并设置代理
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .proxy(proxy)
                .build();


        HttpService httpService = new HttpService(web3jUrl,okHttpClient);
        Web3j web3j = Web3j.build(httpService);

        web3j.blockFlowable(true).subscribe(blockResp -> handleBlock(blockResp.getBlock()));
    }

    @Async
    public void handleBlock(EthBlock.Block b) {
        // 1. 存区块
        BlockEntity block = new BlockEntity();
        block.setBlockNumber(b.getNumber());
        block.setBlockHash(b.getHash());
        block.setParentHash(b.getParentHash());
        block.setTimestamp(b.getTimestamp());
        block.setTxCount(b.getTransactions().size());
        blockService.saveBlock(block);

        // 2. 存交易
        for (EthBlock.TransactionResult<?> txResult : b.getTransactions()) {
            EthBlock.TransactionObject tx = (EthBlock.TransactionObject) txResult.get();

            TransactionEntity t = new TransactionEntity();
            t.setTxHash(tx.getHash());
            t.setBlockNumber(tx.getBlockNumber());
            t.setFromAddress(tx.getFrom());
            t.setToAddress(tx.getTo());
            t.setValue(tx.getValue());
            t.setGas(tx.getGas());
            t.setGasPrice(tx.getGasPrice());
            t.setInput(tx.getInput());

            txService.saveTransaction(t);

            // 3. 异步查询并存日志
            handleReceiptAndLogs(tx.getHash());
        }
    }

    @Async
    public void handleReceiptAndLogs(String txHash) {
        try {
            Web3j web3j = Web3j.build(new WebSocketService("wss://mainnet.infura.io/ws/v3/YOUR_INFURA_PROJECT_ID", true));
            web3j.ethGetTransactionReceipt(txHash).sendAsync().thenAccept(resp -> {
                resp.getTransactionReceipt().ifPresent(receipt -> {
                    List<Log> logs = receipt.getLogs();
                    for (int i = 0; i < logs.size(); i++) {
                        Log l = logs.get(i);
                        logService.saveWeb3jLog(l,txHash);

                    }
                });
            });
        } catch (Exception e) {
            System.err.println("❌ 获取日志失败: " + e.getMessage());
        }
    }
}