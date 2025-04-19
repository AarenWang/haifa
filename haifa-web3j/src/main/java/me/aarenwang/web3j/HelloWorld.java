package me.aarenwang.web3j;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/LFDT-web3j/web3j/tree/main/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 1.7.0.
 */
@SuppressWarnings("rawtypes")
public class HelloWorld extends Contract {
    public static final String BINARY = "60806040523480156200001157600080fd5b50604051620008c9380380620008c98339818101604052810190620000379190620002a4565b80600090805190602001906200004f92919062000057565b50506200035a565b828054620000659062000324565b90600052602060002090601f016020900481019282620000895760008555620000d5565b82601f10620000a457805160ff1916838001178555620000d5565b82800160010185558215620000d5579182015b82811115620000d4578251825591602001919060010190620000b7565b5b509050620000e49190620000e8565b5090565b5b8082111562000103576000816000905550600101620000e9565b5090565b6000604051905090565b600080fd5b600080fd5b600080fd5b600080fd5b6000601f19601f8301169050919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052604160045260246000fd5b620001708262000125565b810181811067ffffffffffffffff8211171562000192576200019162000136565b5b80604052505050565b6000620001a762000107565b9050620001b5828262000165565b919050565b600067ffffffffffffffff821115620001d857620001d762000136565b5b620001e38262000125565b9050602081019050919050565b60005b8381101562000210578082015181840152602081019050620001f3565b8381111562000220576000848401525b50505050565b60006200023d6200023784620001ba565b6200019b565b9050828152602081018484840111156200025c576200025b62000120565b5b62000269848285620001f0565b509392505050565b600082601f8301126200028957620002886200011b565b5b81516200029b84826020860162000226565b91505092915050565b600060208284031215620002bd57620002bc62000111565b5b600082015167ffffffffffffffff811115620002de57620002dd62000116565b5b620002ec8482850162000271565b91505092915050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052602260045260246000fd5b600060028204905060018216806200033d57607f821691505b60208210811415620003545762000353620002f5565b5b50919050565b61055f806200036a6000396000f3fe608060405234801561001057600080fd5b50600436106100415760003560e01c8063368b877214610046578063ce6d41de14610062578063e21f37ce14610080575b600080fd5b610060600480360381019061005b91906103d5565b61009e565b005b61006a6100b8565b60405161007791906104a6565b60405180910390f35b61008861014a565b60405161009591906104a6565b60405180910390f35b80600090805190602001906100b49291906101d8565b5050565b6060600080546100c7906104f7565b80601f01602080910402602001604051908101604052809291908181526020018280546100f3906104f7565b80156101405780601f1061011557610100808354040283529160200191610140565b820191906000526020600020905b81548152906001019060200180831161012357829003601f168201915b5050505050905090565b60008054610157906104f7565b80601f0160208091040260200160405190810160405280929190818152602001828054610183906104f7565b80156101d05780601f106101a5576101008083540402835291602001916101d0565b820191906000526020600020905b8154815290600101906020018083116101b357829003601f168201915b505050505081565b8280546101e4906104f7565b90600052602060002090601f016020900481019282610206576000855561024d565b82601f1061021f57805160ff191683800117855561024d565b8280016001018555821561024d579182015b8281111561024c578251825591602001919060010190610231565b5b50905061025a919061025e565b5090565b5b8082111561027757600081600090555060010161025f565b5090565b6000604051905090565b600080fd5b600080fd5b600080fd5b600080fd5b6000601f19601f8301169050919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052604160045260246000fd5b6102e282610299565b810181811067ffffffffffffffff82111715610301576103006102aa565b5b80604052505050565b600061031461027b565b905061032082826102d9565b919050565b600067ffffffffffffffff8211156103405761033f6102aa565b5b61034982610299565b9050602081019050919050565b82818337600083830152505050565b600061037861037384610325565b61030a565b90508281526020810184848401111561039457610393610294565b5b61039f848285610356565b509392505050565b600082601f8301126103bc576103bb61028f565b5b81356103cc848260208601610365565b91505092915050565b6000602082840312156103eb576103ea610285565b5b600082013567ffffffffffffffff8111156104095761040861028a565b5b610415848285016103a7565b91505092915050565b600081519050919050565b600082825260208201905092915050565b60005b8381101561045857808201518184015260208101905061043d565b83811115610467576000848401525b50505050565b60006104788261041e565b6104828185610429565b935061049281856020860161043a565b61049b81610299565b840191505092915050565b600060208201905081810360008301526104c0818461046d565b905092915050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052602260045260246000fd5b6000600282049050600182168061050f57607f821691505b60208210811415610523576105226104c8565b5b5091905056fea2646970667358221220b89a37166f6f271c8fd24c094ba9fc14f26019df057958de3719631db006a58764736f6c63430008090033";

    private static String librariesLinkedBinary;

    public static final String FUNC_GETMESSAGE = "getMessage";

    public static final String FUNC_MESSAGE = "message";

    public static final String FUNC_SETMESSAGE = "setMessage";

    @Deprecated
    protected HelloWorld(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected HelloWorld(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected HelloWorld(String contractAddress, Web3j web3j, TransactionManager transactionManager,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected HelloWorld(String contractAddress, Web3j web3j, TransactionManager transactionManager,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<String> getMessage() {
        final Function function = new Function(FUNC_GETMESSAGE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> message() {
        final Function function = new Function(FUNC_MESSAGE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> setMessage(String _message) {
        final Function function = new Function(
                FUNC_SETMESSAGE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_message)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static HelloWorld load(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return new HelloWorld(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static HelloWorld load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new HelloWorld(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static HelloWorld load(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return new HelloWorld(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static HelloWorld load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new HelloWorld(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<HelloWorld> deploy(Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider, String _message) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_message)));
        return deployRemoteCall(HelloWorld.class, web3j, credentials, contractGasProvider, getDeploymentBinary(), encodedConstructor);
    }

    public static RemoteCall<HelloWorld> deploy(Web3j web3j, TransactionManager transactionManager,
            ContractGasProvider contractGasProvider, String _message) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_message)));
        return deployRemoteCall(HelloWorld.class, web3j, transactionManager, contractGasProvider, getDeploymentBinary(), encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<HelloWorld> deploy(Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit, String _message) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_message)));
        return deployRemoteCall(HelloWorld.class, web3j, credentials, gasPrice, gasLimit, getDeploymentBinary(), encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<HelloWorld> deploy(Web3j web3j, TransactionManager transactionManager,
            BigInteger gasPrice, BigInteger gasLimit, String _message) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_message)));
        return deployRemoteCall(HelloWorld.class, web3j, transactionManager, gasPrice, gasLimit, getDeploymentBinary(), encodedConstructor);
    }

    public static void linkLibraries(List<Contract.LinkReference> references) {
        librariesLinkedBinary = linkBinaryWithReferences(BINARY, references);
    }

    private static String getDeploymentBinary() {
        if (librariesLinkedBinary != null) {
            return librariesLinkedBinary;
        } else {
            return BINARY;
        }
    }
}
