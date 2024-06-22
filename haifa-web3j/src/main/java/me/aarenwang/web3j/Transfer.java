package me.aarenwang.web3j;


import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;

import static me.aarenwang.web3j.Constant.CREDENTIALS_FILE_PATH;

public class Transfer {

    public static void main(String[] args) throws Exception {
        Web3j web3 = Web3j.build(new HttpService("http://localhost:7545/"));  // defaults to http://localhost:8545/
        Web3ClientVersion web3ClientVersion = web3.web3ClientVersion().send();
        String clientVersion = web3ClientVersion.getWeb3ClientVersion();
        System.out.println(clientVersion);

        BigInteger blockNumber  = web3.ethBlockNumber().send().getBlockNumber();
        String account = web3.ethAccounts().send().getAccounts().get(0);
        BigInteger gasPrice = web3.ethGasPrice().send().getGasPrice();
        BigInteger balance = web3.ethGetBalance(account, DefaultBlockParameter.valueOf(blockNumber)).send().getBalance();
        BigDecimal etherBalance = Convert.fromWei(new BigDecimal(balance), Convert.Unit.ETHER);

        System.out.printf("account[0]=%s BlockNumber=%d gasPrice=%d balance=%s,\n",account,blockNumber,gasPrice,etherBalance.toPlainString());

        transferEtherEIP1559(args,web3);

    }



    public static void transferEtherEIP1559(String[] args, Web3j web3j) throws Exception {
        if(args.length < 2){
            System.out.println("please set password and destAddress");
            return;
        }
        String password = args[0]; //钱包密码
        String destAddress = args[1]; //目标地址

        String userHomeDir = System.getProperty("user.home");
        File credentialsFile = CREDENTIALS_FILE_PATH.toFile();

        //Credentials credentials = WalletUtils.loadCredentials(password,credentialsFile);
        String privateKey = args[0];
        Credentials credentials = Credentials.create(privateKey);        BigDecimal sendValue =  Convert.toWei("0.5",Convert.Unit.ETHER); //写死0.01ETH
        BigInteger gasLimit = new BigInteger("21000"); // transfer的gasLimit写死21000
        BigInteger maxPriorityFeePerGas = web3j.ethMaxPriorityFeePerGas().send().getMaxPriorityFeePerGas();
        BigInteger baseFeePerGas = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST,false).send().getBlock().getBaseFeePerGas();
        BigInteger maxFeePerGas = baseFeePerGas.multiply(new BigInteger("2")).add(maxPriorityFeePerGas);

        RemoteCall<TransactionReceipt> receiptRemoteCall =  org.web3j.tx.Transfer.sendFundsEIP1559(web3j,credentials,destAddress,sendValue,Convert.Unit.WEI,gasLimit,maxPriorityFeePerGas,maxFeePerGas);
        TransactionReceipt receipt = receiptRemoteCall.send();
        System.out.println(receipt.toString());
    }







}
