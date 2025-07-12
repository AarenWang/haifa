package me.aarenwang.web3j.train;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.http.HttpService;

public class BlockMain {

    public static void main(String[] args) {
        String rpcUrl = args[0];
        String hash = args[1];
        HttpService httpService = new HttpService(rpcUrl);
        Web3j web3j = Web3j.build(httpService);

        web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf("latest"),false).sendAsync().thenAccept(block -> {
            System.out.println("Block Details:");
            System.out.println("Number: " + block.getBlock().getNumber());
            System.out.println("Hash: " + block.getBlock().getHash());
            System.out.println("Parent Hash: " + block.getBlock().getParentHash());
            System.out.println("Timestamp: " + block.getBlock().getTimestamp());
            System.out.println("Transactions: " + block.getBlock().getTransactions());
        }).exceptionally(ex -> {
            System.err.println("Error fetching block details: " + ex.getMessage());
            return null;
        });
    }
}
