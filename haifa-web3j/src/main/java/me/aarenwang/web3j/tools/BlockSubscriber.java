package me.aarenwang.web3j.tools;

import org.apache.commons.cli.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.websocket.WebSocketService;
import org.web3j.protocol.core.methods.response.EthBlock;
import io.reactivex.disposables.Disposable;

import java.net.ConnectException;
import java.util.List;

public class BlockSubscriber {


    private static String getProjectId(String[] args){
        Options options = new Options();
        Option opt = new Option("p", "project_id", true, "Node project ID");
        opt.setRequired(true);
        options.addOption(opt);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
            return cmd.getOptionValue("project_id");
        } catch (ParseException e) {
            System.err.println("参数解析错误: " + e.getMessage());
            formatter.printHelp("BlockSubscriberCLI", options);
            System.exit(1);
            return null;
        }

    }

    public static void main(String[] args) throws ConnectException {
        // 使用 WebSocketService 连接以太坊节点（使用 Infura、Alchemy 或本地 Geth）
        String projectId = getProjectId(args);

        String websocketUrl = "wss://eth-mainnet.g.alchemy.com/v2/" + projectId;
        WebSocketService webSocketService = new WebSocketService(websocketUrl, true);
        webSocketService.connect();

        Web3j web3j = Web3j.build(webSocketService);

        // false 表示返回完整区块对象（包括交易）
        Disposable subscription = web3j.blockFlowable(true).subscribe(block -> {
            EthBlock.Block b = block.getBlock();
            System.out.println("📦 区块高度: " + b.getNumber());
            System.out.println("⛓️ 区块哈希: " + b.getHash());
            System.out.println("⏱️ 时间戳: " + b.getTimestamp());
            System.out.println("🔢 交易数: " + b.getTransactions().size());
            System.out.println("----------------------------------");

            // 遍历区块中的所有交易
            List<EthBlock.TransactionResult> transactions = b.getTransactions();
            for (EthBlock.TransactionResult<?> txResult : transactions) {
                EthBlock.TransactionObject tx = (EthBlock.TransactionObject) txResult.get();

                System.out.println("🧾 交易哈希: " + tx.getHash());
                System.out.println("  🔸 From: " + tx.getFrom());
                System.out.println("  🔹 To:   " + tx.getTo());
                System.out.println("  💰 Value (Wei): " + tx.getValue());
                System.out.println("  🧾 Input Data: " + tx.getInput());
                System.out.println("  🚀 Gas Price: " + tx.getGasPrice());
                System.out.println("  ⛽ Gas Limit: " + tx.getGas());
                System.out.println("  ---");

                try {
                    TransactionReceipt receipt = web3j.ethGetTransactionReceipt(tx.getHash())
                            .send()
                            .getTransactionReceipt()
                            .orElse(null);

                    if (receipt != null) {
                        List<Log> logs = receipt.getLogs();
                        System.out.println("  📜 日志条数: " + logs.size());

                        for (int i = 0; i < logs.size(); i++) {
                            Log log = logs.get(i);
                            System.out.println("    🧷 日志 #" + i);
                            System.out.println("      🔘 合约地址: " + log.getAddress());
                            System.out.println("      🏷️ Topics:");
                            for (String topic : log.getTopics()) {
                                System.out.println("        - " + topic);
                            }
                            System.out.println("      🧩 Data: " + log.getData());
                        }
                    } else {
                        System.out.println("  ⚠️ 未获取到交易回执");
                    }
                } catch (Exception e) {
                    System.out.println("  ❌ 获取日志失败: " + e.getMessage());
                }

            }

            System.out.println("===================================");
        },error -> {
            System.err.println("🚨 订阅发生错误: " + error.getMessage());
            error.printStackTrace();
            // 可在此触发重连逻辑
        });

        // 订阅将一直进行，直到手动取消订阅
    }
}
