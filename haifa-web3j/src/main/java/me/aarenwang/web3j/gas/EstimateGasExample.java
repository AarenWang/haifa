package me.aarenwang.web3j.gas;

//import org.web3j.protocol.Web3j;
//import org.web3j.protocol.http.HttpService;
//import org.web3j.protocol.core.methods.request.Transaction;
//import org.web3j.protocol.core.methods.response.EthEstimateGas;
//import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

public class EstimateGasExample {

    public static void main(String[] args) throws Exception {
        // 1. 创建 Web3j 实例
        Web3j web3j = Web3j.build(new HttpService("https://mainnet.infura.io/v3/YOUR_PROJECT_ID"));

        // 2. 准备参数
        String fromAddress = "0xYourAddress";
        String contractAddress = "0xTokenContractAddress";
        String toAddress = "0xRecipientAddress";
        BigInteger amount = BigInteger.valueOf(1_000_000_000_000_000_000L); // 1 Token（按18位精度）

        // 3. 构造合约函数对象
        Function function = new Function(
                "transfer",
                Arrays.asList(new Address(toAddress), new Uint256(amount)),
                Collections.emptyList()
        );
        String encodedFunction = FunctionEncoder.encode(function);

        // 4. 获取当前 gasPrice
        EthGasPrice ethGasPrice = web3j.ethGasPrice().send();
        BigInteger gasPrice = ethGasPrice.getGasPrice();

        // 5. 创建交易对象并估算 gasLimit
        Transaction transaction = Transaction.createFunctionCallTransaction(
                fromAddress,
                null,               // nonce 可为空
                gasPrice,
                null,               // gasLimit 先为空，系统会自动估算
                contractAddress,
                BigInteger.ZERO,    // ETH value 发送为 0
                encodedFunction
        );

        EthEstimateGas estimateGas = web3j.ethEstimateGas(transaction).send();
        BigInteger gasLimit = estimateGas.getAmountUsed();

        // 6. 输出估算结果
        BigInteger totalGasCost = gasPrice.multiply(gasLimit);
        BigDecimal totalCostEth = new BigDecimal(totalGasCost).divide(BigDecimal.TEN.pow(18));

        System.out.println("Gas Price (wei): " + gasPrice);
        System.out.println("Estimated Gas Limit: " + gasLimit);
        System.out.println("Total Gas Cost (wei): " + totalGasCost);
        System.out.println("Total Cost in ETH: " + totalCostEth);
    }
}

