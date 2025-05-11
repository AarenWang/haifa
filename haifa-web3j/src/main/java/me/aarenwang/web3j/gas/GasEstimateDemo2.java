package me.aarenwang.web3j.gas;

import okhttp3.OkHttpClient;
import org.apache.commons.cli.*;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint160;
import org.web3j.abi.datatypes.generated.Uint24;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;
import java.util.Collections;

public class GasEstimateDemo2 {

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        options.addOption(new Option("f", "frome", true, "your address"));
        options.addOption(new Option("u", "url", true, "JSON-RPC URL"));
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        String from = cmd.getOptionValue("f");
        String url = cmd.getOptionValue("u");

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .proxy(new java.net.Proxy(java.net.Proxy.Type.HTTP, new java.net.InetSocketAddress("127.0.0.1", 1081)))
                .build();
        Web3j web3j = Web3j.build(new HttpService(url,httpClient));

        String router = "0xE592427A0AEce92De3Edee1F18E0157C05861564";

        // 构造 ExactInputSingleParams 结构体参数
        Address tokenIn = new Address("0x6B175474E89094C44Da98b954EedeAC495271d0F"); // DAI
        Address tokenOut = new Address("0x55d398326f99059fF775485246999027B3197955"); // USDT
        Uint24 fee = new Uint24(3000); // 0.3%
        Address recipient = new Address(from);
        Uint256 deadline = new Uint256(BigInteger.valueOf(System.currentTimeMillis() / 1000 + 600)); // 10分钟后
        Uint256 amountIn = new Uint256(BigInteger.valueOf(1_000_000_000_000_000_000L)); // 1 DAI
        Uint256 amountOutMinimum = new Uint256(BigInteger.ZERO); // 预估不执行兑换，允许最小输出为 0
        Uint160 sqrtPriceLimitX96 = new Uint160(BigInteger.ZERO); // 无价格限制

        DynamicStruct params = new DynamicStruct(
                tokenIn,
                tokenOut,
                fee,
                recipient,
                deadline,
                amountIn,
                amountOutMinimum,
                sqrtPriceLimitX96
        );

        // 构造函数调用对象
        Function function = new Function(
                "exactInputSingle",
                Collections.singletonList(params),
                Collections.singletonList(new TypeReference<Uint256>() {
                })
        );

        String encodedFunction = FunctionEncoder.encode(function);

        // 获取当前 gasPrice
        EthGasPrice gasPriceResponse = web3j.ethGasPrice().send();
        BigInteger gasPrice = gasPriceResponse.getGasPrice();

        // 构造预估交易对象（不发送）
        Transaction tx = Transaction.createFunctionCallTransaction(
                from,
                null,
                gasPrice,
                null,
                router,
                BigInteger.ZERO,
                encodedFunction
        );

        EthEstimateGas estimate = web3j.ethEstimateGas(tx).send();

        if (estimate.hasError()) {
            System.err.println("❌ Gas estimation failed: " + estimate.getError().getMessage());
        } else {
            BigInteger gasUsed = estimate.getAmountUsed();
            System.out.println("✅ Estimated Gas: " + gasUsed);
        }
    }

}
