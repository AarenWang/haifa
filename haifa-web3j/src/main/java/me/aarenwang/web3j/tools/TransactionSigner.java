package me.aarenwang.web3j.tools;

import java.math.BigInteger;

import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.ChainIdLong;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;



public class TransactionSigner {

    public static void main(String[] args) throws Exception {
        Web3j web3j = Web3j.build(new HttpService("https://mainnet.infura.io/v3/YOUR_KEY"));
        Credentials credentials = WalletUtils.loadCredentials("yourStrongPassword", "UTC--2025-04-29T09-47-38.725322000Z--1a6e71e7b4c2c768af3e0960372bbeb415995a8b.json");


        // Legacy 格式
        String legacyTx = signLegacyTransaction(
                web3j, credentials,
                "0xRecipient", Convert.toWei("0.01", Convert.Unit.ETHER).toBigInteger(),
                BigInteger.valueOf(21000),
                web3j.ethGasPrice().send().getGasPrice(),
                ChainIdLong.MAINNET
        );

        // EIP-1559 格式
        String eip1559Tx = signEIP1559Transaction(
                web3j, credentials,
                "0xRecipient", Convert.toWei("0.01", Convert.Unit.ETHER).toBigInteger(),
                BigInteger.valueOf(21000),
                Convert.toWei("1", Convert.Unit.GWEI).toBigInteger(),     // priority fee
                Convert.toWei("30", Convert.Unit.GWEI).toBigInteger(),    // max fee
                ChainIdLong.MAINNET
        );


        // 打印签名后的十六进制交易
        System.out.println("Legacy 签名交易: " + legacyTx);
        System.out.println("EIP-1559 签名交易: " + eip1559Tx);
    }


    public static String signLegacyTransaction(
            Web3j web3j,
            Credentials credentials,
            String toAddress,
            BigInteger valueWei,
            BigInteger gasLimit,
            BigInteger gasPrice,
            long chainId
    ) throws Exception {
        // 获取 nonce
        BigInteger nonce = web3j.ethGetTransactionCount(
                credentials.getAddress(),
                DefaultBlockParameterName.LATEST
        ).send().getTransactionCount();

        // 构造 legacy 类型交易
        RawTransaction rawTransaction = RawTransaction.createEtherTransaction(
                nonce, gasPrice, gasLimit, toAddress, valueWei
        );

        // 签名交易
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials);
        return Numeric.toHexString(signedMessage);
    }


    public static String signEIP1559Transaction(
            Web3j web3j,
            Credentials credentials,
            String toAddress,
            BigInteger valueWei,
            BigInteger gasLimit,
            BigInteger maxPriorityFeePerGas,
            BigInteger maxFeePerGas,
            long chainId
    ) throws Exception {
        // 获取 nonce
        BigInteger nonce = web3j.ethGetTransactionCount(
                credentials.getAddress(),
                DefaultBlockParameterName.LATEST
        ).send().getTransactionCount();

        // 构造 EIP-1559 类型交易
        RawTransaction rawTransaction = RawTransaction.createEtherTransaction(
                chainId,
                nonce,
                gasLimit,
                toAddress,
                valueWei,
                maxPriorityFeePerGas,
                maxFeePerGas
                // access list，可为空
        );

        // 签名 EIP-1559 交易（新格式）
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        return Numeric.toHexString(signedMessage);
    }


}
