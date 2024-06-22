package me.aarenwang.web3j;

import org.web3j.crypto.*;
import org.web3j.model.SendEther;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.gas.StaticEIP1559GasProvider;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

import java.util.concurrent.ExecutionException;

public class DeployContract {

    public static void main(String[] args) throws Exception {
        Web3j web3j = Web3j.build(new HttpService("http://localhost:7545/"));

        if(args.length < 2){
            System.out.println("please set password and destAddress");
            return;
        }
        String password = args[0];
        String destAddress = args[1];
        String privateKey = args[2];

        File credentialsFile = Constant.CREDENTIALS_FILE_PATH.toFile();
        //Credentials credentials = WalletUtils.loadCredentials(password,credentialsFile);
        //Credentials credentials = Constant.getCredentialsFromFile(password);
        Credentials credentials = Constant.getCredentialsFromPrivateKey(privateKey);
        String address = credentials.getAddress();

        //deployWithEIP1559(web3j, credentials, address);
        deployWithStub(web3j,credentials,address);
        //deployWithLegacy(web3j,credentials,address);
    }


    /**
     *
     * @param web3j
     * @param credentials
     * @param address
     * @throws Exception
     */
    private static void deployWithStub(Web3j web3j, Credentials credentials, String address) throws Exception {

        long chainId = web3j.ethChainId().send().getChainId().longValue();
        BigInteger maxPriorityFeePerGas = web3j.ethMaxPriorityFeePerGas().send().getMaxPriorityFeePerGas();
        BigInteger baseFeePerGas = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST,false).send().getBlock().getBaseFeePerGas();
        //BigInteger maxFeePerGas = baseFeePerGas.multiply(new BigInteger("2")).add(maxPriorityFeePerGas);
        BigInteger maxFeePerGas = null;


        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                address, DefaultBlockParameterName.LATEST).send();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();
        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();


        Transaction transaction  = Transaction.createContractTransaction(address,nonce,gasPrice,SendEther.BINARY);
        BigInteger gasLimit = web3j.ethEstimateGas(transaction).send().getAmountUsed();

        //StaticEIP1559GasProvider  contractGasProvider = new StaticEIP1559GasProvider(chainId,maxPriorityFeePerGas,maxFeePerGas,gasLimit);
        DefaultGasProvider defaultGasProvider = new DefaultGasProvider();
        RemoteCall<SendEther> remoteCall =  SendEther.deploy(web3j,credentials, defaultGasProvider,new BigInteger("0"));
        String contractAddress = remoteCall.send().getContractAddress();
        System.out.println("contractAddress="+contractAddress);
    }

    private static void deployWithLegacy(Web3j web3j, Credentials credentials, String address) throws InterruptedException, ExecutionException, IOException {
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                address, DefaultBlockParameterName.LATEST).send();

        BigInteger nonce = ethGetTransactionCount.getTransactionCount();
        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
        BigInteger rawGasLimit = new BigInteger("10000000");

        //Transaction transaction  = Transaction.createContractTransaction(address,nonce,gasPrice,SendEther.BINARY);
        Transaction transaction  = Transaction.createContractTransaction(address,nonce,gasPrice,rawGasLimit,new BigInteger("0"),SendEther.BINARY);
        BigInteger gasLimit = web3j.ethEstimateGas(transaction).send().getAmountUsed();
        System.out.println("创建合约预估gasLimit="+gasLimit);

        long chainId = web3j.ethChainId().send().getChainId().longValue();
        String toZero = "0x0000000000000000000000000000000000000000";

        TransactionManager txManager = new RawTransactionManager(web3j,credentials,chainId);
        EthSendTransaction sendTx = txManager.sendTransaction(gasPrice,gasLimit,toZero,SendEther.BINARY,BigInteger.ZERO,true);
        if(sendTx.hasError()){
            String message=sendTx.getError().getMessage();
            System.out.println("transaction failed,info:"+message);

        }else {
            TransactionReceipt receipt = web3j.ethGetTransactionReceipt(sendTx.getTransactionHash()).send().getResult();
            if (receipt != null) {
                System.out.println("合约地址:"+receipt.getContractAddress());
                System.out.println(receipt.toString());
            } else {
                org.web3j.protocol.core.methods.response.Transaction tx = web3j.ethGetTransactionByHash(sendTx.getTransactionHash()).send().getTransaction().get();
                System.out.println(tx.getFrom()+":"+tx.getTo()+":"+tx.getGasPrice()+":"+tx.getValue());
            }
        }


    }

    private static void deployWithEIP1559(Web3j web3j, Credentials credentials, String address) throws InterruptedException, ExecutionException, IOException {
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                address, DefaultBlockParameterName.LATEST).send();

        BigInteger nonce = ethGetTransactionCount.getTransactionCount();
        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();


        //Transaction transaction  = Transaction.createContractTransaction(address,nonce,gasPrice,SendEther.BINARY);
        Transaction transaction  = Transaction.createContractTransaction(address,nonce,gasPrice,null,BigInteger.ZERO,"0x"+SendEther.BINARY);
        String value = transaction.getValue();
        BigInteger gasLimit = web3j.ethEstimateGas(transaction).send().getAmountUsed();
        System.out.println("创建合约预估gasLimit="+gasLimit);

        BigInteger maxPriorityFeePerGas = web3j.ethMaxPriorityFeePerGas().send().getMaxPriorityFeePerGas();
        BigInteger baseFeePerGas = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST,false).send().getBlock().getBaseFeePerGas();
        BigInteger maxFeePerGas = baseFeePerGas.multiply(new BigInteger("2")).add(maxPriorityFeePerGas);

        long chainId = web3j.ethChainId().send().getChainId().longValue();
        String toZero = "0x0000000000000000000000000000000000000000";

        TransactionManager transactionManager = new RawTransactionManager(web3j, credentials);
        EthSendTransaction ethSendTransaction = transactionManager.sendEIP1559Transaction(chainId,maxPriorityFeePerGas,maxFeePerGas,gasLimit,toZero,SendEther.BINARY,BigInteger.ZERO,true);

        String transactionHash = ethSendTransaction.getTransactionHash();
        if(ethSendTransaction.hasError()){
            String message=ethSendTransaction.getError().getMessage();
            System.out.println("transaction failed,info:"+message);

        }else {
            TransactionReceipt receipt = web3j.ethGetTransactionReceipt(transactionHash).send().getResult();
            if (receipt != null) {
                System.out.println(receipt);
                System.out.println(receipt.toString());
                receipt.getContractAddress();
            } else{
                System.out.println("ethGetTransactionReceipt is null");
            }
        }
    }

}
