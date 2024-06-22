package me.aarenwang.web3j;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.model.SendEther;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.StaticEIP1559GasProvider;
import org.web3j.utils.Convert;

import java.io.File;
import java.math.BigInteger;


public class CallContract {

    public static void main(String[] args) throws Exception{
        // 0x496342746AA8e3ab72924253701882efCddde247
        String contractAddress = "0x496342746AA8e3ab72924253701882efCddde247";

        Web3j web3j = Web3j.build(new HttpService("http://localhost:8545/"));

        if(args.length < 2){
            System.out.println("please set password and destAddress");
            return;
        }
        String password = args[0];
        String destAddress = args[1];


        File credentialsFile = Constant.CREDENTIALS_FILE_PATH.toFile();
        Credentials credentials = WalletUtils.loadCredentials(password,credentialsFile);
        String address = credentials.getAddress();

        long chainId = web3j.ethChainId().send().getChainId().longValue();
        BigInteger maxPriorityFeePerGas = web3j.ethMaxPriorityFeePerGas().send().getMaxPriorityFeePerGas();
        BigInteger baseFeePerGas = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST,false).send().getBlock().getBaseFeePerGas();
        BigInteger maxFeePerGas = baseFeePerGas.multiply(new BigInteger("2")).add(maxPriorityFeePerGas);


        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                address, DefaultBlockParameterName.LATEST).sendAsync().get();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();
        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();

        BigInteger balance1 =  web3j.ethGetBalance(destAddress,DefaultBlockParameterName.LATEST).send().getBalance();

        Transaction transaction  = Transaction.createContractTransaction(address,nonce,gasPrice,SendEther.BINARY);
        BigInteger gasLimit = web3j.ethEstimateGas(transaction).send().getAmountUsed().multiply(new BigInteger("2"));

        StaticEIP1559GasProvider contractGasProvider = new StaticEIP1559GasProvider(chainId,maxFeePerGas,maxPriorityFeePerGas,gasLimit);

        SendEther sendEther = SendEther.load(contractAddress,web3j,credentials,contractGasProvider);
        RemoteFunctionCall<TransactionReceipt>   functionCall = sendEther.sendViaCall(destAddress, Convert.toWei("0.01",Convert.Unit.ETHER).toBigInteger());
        String encode = functionCall.encodeFunctionCall();
        System.out.println("encode="+encode);
        TransactionReceipt receipt = functionCall.send();
        System.out.println(receipt);

        BigInteger balance2 =  web3j.ethGetBalance(destAddress,DefaultBlockParameterName.LATEST).send().getBalance();

        System.out.printf("balance1=%d,balance2=%d \n",balance1,balance2);

    }
}
