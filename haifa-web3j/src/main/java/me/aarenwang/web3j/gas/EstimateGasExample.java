package me.aarenwang.web3j.gas;

import okhttp3.OkHttpClient;
import org.apache.commons.cli.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGasPrice;
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

        final Options options = new Options();
        options.addOption(new Option("f", "from", true, "from address"));
        options.addOption(new Option("t", "to", true, "to address"));
        options.addOption(new Option("c", "contract", true, "ERC-20 contract address"));
        options.addOption(new Option("u", "url", true, "JSPN RPC Node URL"));

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("EstimateGasExample", options);
        CommandLine line = parser.parse(options, args);
        Arrays.stream(line.getOptions()).forEach(option -> {
            System.out.println("Option: " + option.getOpt() + " Value: " + option.getValue());
        });

        //line.getArgList().forEach(System.out::println);

        String from = line.getOptionValue("from");
        String to = line.getOptionValue("to");
        String contract = line.getOptionValue("contract");
        String url = line.getOptionValue("url");


        // 1. 创建 Web3j 实例
        java.net.Proxy proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, new java.net.InetSocketAddress("127.0.0.1",1081));

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .proxy(proxy)
                .build();
        Web3j web3j = Web3j.build(new HttpService(url,httpClient));

        // 2. 准备参数
        BigInteger amount = BigInteger.valueOf(1_000_000L); // 1 Token（按18位精度）

        // 3. 构造合约函数对象
        Function function = new Function(
                "transfer",
                Arrays.asList(new Address(to), new Uint256(amount)),
                Collections.emptyList()
        );
        String encodedFunction = FunctionEncoder.encode(function);

        // 4. 获取当前 gasPrice
        EthGasPrice ethGasPrice = web3j.ethGasPrice().send();
        BigInteger gasPrice = ethGasPrice.getGasPrice();

        // 5. 创建交易对象并估算 gasLimit
        Transaction transaction = Transaction.createFunctionCallTransaction(
                from,
                null,               // nonce 可为空
                gasPrice,
                null,               // gasLimit 先为空，系统会自动估算
                contract,
                BigInteger.ONE,    // ETH value 发送为 0
                encodedFunction
        );

        EthEstimateGas estimateGas = web3j.ethEstimateGas(transaction).send();
        if (estimateGas.hasError()) {
            System.err.println("Gas estimation failed: " + estimateGas.getError().getMessage());
            return;
        }
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

