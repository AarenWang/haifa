package me.aarenwang.web3j.erc20;

import okhttp3.OkHttpClient;
import org.apache.commons.cli.*;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Erc20Utils {

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(new Option("t", "token", true, "token address"));
        options.addOption(new Option("o", "owner", true, "owner address"));
        options.addOption(new Option("u", "url", true, "JSON-RPC URL"));
        options.addOption(new Option("s", "spender", true, "spender address"));
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        String tokenAddress = cmd.getOptionValue("t");
        String owner = cmd.getOptionValue("o");
        String url = cmd.getOptionValue("u");
        String spender = cmd.getOptionValue("s");

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .proxy(new java.net.Proxy(java.net.Proxy.Type.HTTP, new java.net.InetSocketAddress("127.0.0.1", 1081)))
                .build();
        Web3j web3j = Web3j.build(new HttpService(url,httpClient));

        BigInteger erc20Balance = getErc20Balance(web3j,tokenAddress, owner);
        System.out.printf("ERC20 Balance: %s wei, %s gwei \n",erc20Balance,erc20Balance.divide(BigInteger.TEN.pow(18)));

        BigInteger allowance = getErc20Allowance(web3j,tokenAddress, owner, spender);
        System.out.printf("ERC20 Allowance: %s wei, %s gwei \n",allowance,allowance.divide(BigInteger.TEN.pow(18)));

    }

    // 查询 ERC20 余额
    public static BigInteger getErc20Balance(Web3j web3j, String tokenAddress, String owner) throws Exception {
        Function function = new Function(
                "balanceOf",
                Collections.singletonList(new Address(owner)),
                Collections.singletonList(new TypeReference<Uint256>() {})
        );

        String data = FunctionEncoder.encode(function);

        Transaction tx = Transaction.createEthCallTransaction(owner, tokenAddress, data);
        EthCall response = web3j.ethCall(tx, DefaultBlockParameterName.LATEST).send();

        if (response.isReverted()) {
            System.err.println("⚠️ balanceOf call reverted: " + response.getRevertReason());
            return BigInteger.ZERO;
        }

        List<Type> decoded = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        return decoded.isEmpty() ? BigInteger.ZERO : (BigInteger) decoded.get(0).getValue();
    }

    // 查询 ERC20 授权额度
    public static BigInteger getErc20Allowance(Web3j web3j, String tokenAddress, String owner, String spender) throws Exception {
        Function function = new Function(
                "allowance",
                Arrays.asList(new Address(owner), new Address(spender)),
                Collections.singletonList(new TypeReference<Uint256>() {})
        );

        String data = FunctionEncoder.encode(function);

        Transaction tx = Transaction.createEthCallTransaction(owner, tokenAddress, data);
        EthCall response = web3j.ethCall(tx, DefaultBlockParameterName.LATEST).send();

        if (response.isReverted()) {
            System.err.println("⚠️ allowance call reverted: " + response.getRevertReason());
            return BigInteger.ZERO;
        }

        List<Type> decoded = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        return decoded.isEmpty() ? BigInteger.ZERO : (BigInteger) decoded.get(0).getValue();
    }
}

