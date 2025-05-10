package me.aarenwang.web3j.gas;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGasPrice;

import java.math.BigDecimal;
import java.math.BigInteger;

public class GasEstimator {

    /**
     * 估算任意合约函数的 gas 消耗和费用
     */
    public static void estimateGasForFunction(
            Web3j web3j,
            String from,
            String contractAddress,
            Function function
    ) throws Exception {

        // 1. 编码函数
        String encodedFunction = FunctionEncoder.encode(function);

        // 2. 获取 gasPrice
        EthGasPrice gasPriceResponse = web3j.ethGasPrice().send();
        BigInteger gasPrice = gasPriceResponse.getGasPrice();

        // 3. 构造交易用于估算
        Transaction tx = Transaction.createFunctionCallTransaction(
                from,
                null,               // nonce
                gasPrice,
                null,               // gas limit (estimate only)
                contractAddress,
                BigInteger.ZERO,    // no ETH value
                encodedFunction
        );

        EthEstimateGas estimate = web3j.ethEstimateGas(tx).send();

        if (estimate.hasError()) {
            System.err.println("⚠️ Gas estimation failed: " + estimate.getError().getMessage());
            return;
        }

        BigInteger gasLimit = estimate.getAmountUsed();
        BigInteger totalCost = gasPrice.multiply(gasLimit);
        BigDecimal totalCostEth = new BigDecimal(totalCost).divide(BigDecimal.TEN.pow(18));

        System.out.println("Gas Price (wei): " + gasPrice);
        System.out.println("Estimated Gas Limit: " + gasLimit);
        System.out.println("Total Gas Fee (wei): " + totalCost);
        System.out.println("Total Cost in ETH: " + totalCostEth);
    }
}
