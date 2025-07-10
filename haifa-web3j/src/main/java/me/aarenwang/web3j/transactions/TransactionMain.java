package me.aarenwang.web3j.transactions;

import com.alibaba.fastjson2.JSON;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;


public class TransactionMain {

    public static void main(String[] args) throws IOException {
        String rpcUrl = args[0];
        String hash = args[1];
        HttpService httpService = new HttpService(rpcUrl);
        Web3j web3j = Web3j.build(httpService);
        web3j.ethGetTransactionByHash(hash).send().getTransaction().ifPresentOrElse(transaction -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Transaction Details:\n");
            sb.append("Hash: ").append(transaction.getHash()).append("\n");
            sb.append("nonce: ").append(transaction.getNonce()).append("\n");
            sb.append("Block Hash: ").append(transaction.getBlockHash()).append("\n");
            sb.append("Block Number: ").append(transaction.getBlockNumber()).append("\n");
            sb.append("chainId: ").append(transaction.getChainId()).append("\n");
            sb.append("transactionIndex: ").append(transaction.getTransactionIndex()).append("\n");
            sb.append("From: ").append(transaction.getFrom()).append("\n");
            sb.append("To: ").append(transaction.getTo()).append("\n");
            sb.append("Value: ").append(transaction.getValue()).append("\n");
            sb.append("GasPrice: ").append(transaction.getGasPrice()).append("\n");
            sb.append("Gas: ").append(transaction.getGas()).append("\n");
            sb.append("Input: ").append(transaction.getInput()).append("\n");
            sb.append("creates: ").append(transaction.getCreates()).append("\n");
            sb.append("PublicKey: ").append(transaction.getPublicKey()).append("\n");
            sb.append("Raw: ").append(transaction.getRaw()).append("\n");
            sb.append("R: ").append(transaction.getR()).append("\n");
            sb.append("S: ").append(transaction.getS()).append("\n");
            sb.append("V: ").append(transaction.getV()).append("\n");
            sb.append("Type: ").append(transaction.getType()).append("\n");
            sb.append("MaxFeePerGas: ").append(transaction.getMaxFeePerGas()).append("\n");
            sb.append("MaxPriorityFeePerGas: ").append(transaction.getMaxPriorityFeePerGas()).append("\n");
            sb.append("AccessList: ").append(transaction.getAccessList()).append("\n");
            System.out.println(sb.toString());

        },
                () -> {
                    System.out.println("Transaction not found for hash: " + hash);
                });

        web3j.ethGetTransactionReceipt(hash).send().getTransactionReceipt().ifPresentOrElse(receipt -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Transaction Receipt Details:\n");
            sb.append("Transaction Hash: ").append(receipt.getTransactionHash()).append("\n");
            sb.append("Block Hash: ").append(receipt.getBlockHash()).append("\n");
            sb.append("Block Number: ").append(receipt.getBlockNumber()).append("\n");
            sb.append("Cumulative Gas Used: ").append(receipt.getCumulativeGasUsed()).append("\n");
            sb.append("Gas Used: ").append(receipt.getGasUsed()).append("\n");
            sb.append("Contract Address: ").append(receipt.getContractAddress()).append("\n");
            sb.append("Logs: ").append(JSON.toJSONString(receipt.getLogs())).append("\n");
            System.out.println(sb.toString());
        }, () -> {
            System.out.println("Transaction receipt not found for hash: " + hash);
        });

        web3j.shutdown();
    }
}
