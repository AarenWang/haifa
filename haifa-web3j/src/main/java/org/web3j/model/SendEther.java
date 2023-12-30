package org.web3j.model;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
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
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.10.3.
 */
@SuppressWarnings("rawtypes")
public class SendEther extends Contract {
    public static final String BINARY = "608060405261024f806100115f395ff3fe608060405260043610610033575f3560e01c806374be480614610037578063830c29ae1461004c578063d27f8e661461005f575b5f80fd5b61004a6100453660046101d1565b610072565b005b61004a61005a3660046101d1565b6100e6565b61004a61006d3660046101f1565b610183565b6040515f906001600160a01b038316903480156108fc029184818181858888f193505050509050806100e25760405162461bcd60e51b81526020600482015260146024820152732330b4b632b2103a379039b2b7321022ba3432b960611b60448201526064015b60405180910390fd5b5050565b5f80826001600160a01b0316346040515f6040518083038185875af1925050503d805f8114610130576040519150601f19603f3d011682016040523d82523d5f602084013e610135565b606091505b50915091508161017e5760405162461bcd60e51b81526020600482015260146024820152732330b4b632b2103a379039b2b7321022ba3432b960611b60448201526064016100d9565b505050565b6040516001600160a01b0383169082156108fc029083905f818181858888f1935050505015801561017e573d5f803e3d5ffd5b80356001600160a01b03811681146101cc575f80fd5b919050565b5f602082840312156101e1575f80fd5b6101ea826101b6565b9392505050565b5f8060408385031215610202575f80fd5b61020b836101b6565b94602093909301359350505056fea264697066735822122039a6c2e921355c5c825bc30500499a73386707e53f11b54a852f75f370c4fd6364736f6c63430008140033";

    public static final String FUNC_SENDVIACALL = "sendViaCall";

    public static final String FUNC_SENDVIASEND = "sendViaSend";

    public static final String FUNC_SENDVIATRANSFER = "sendViaTransfer";

    @Deprecated
    protected SendEther(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected SendEther(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected SendEther(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected SendEther(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<TransactionReceipt> sendViaCall(String _to, BigInteger weiValue) {
        final Function function = new Function(
                FUNC_SENDVIACALL, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _to)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function, weiValue);
    }

    public RemoteFunctionCall<TransactionReceipt> sendViaSend(String _to, BigInteger weiValue) {
        final Function function = new Function(
                FUNC_SENDVIASEND, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _to)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function, weiValue);
    }

    public RemoteFunctionCall<TransactionReceipt> sendViaTransfer(String _to, BigInteger amount, BigInteger weiValue) {
        final Function function = new Function(
                FUNC_SENDVIATRANSFER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _to), 
                new org.web3j.abi.datatypes.generated.Uint256(amount)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function, weiValue);
    }

    @Deprecated
    public static SendEther load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new SendEther(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static SendEther load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new SendEther(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static SendEther load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new SendEther(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static SendEther load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new SendEther(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<SendEther> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, BigInteger initialWeiValue) {
        return deployRemoteCall(SendEther.class, web3j, credentials, contractGasProvider, BINARY, "", initialWeiValue);
    }

    public static RemoteCall<SendEther> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, BigInteger initialWeiValue) {
        return deployRemoteCall(SendEther.class, web3j, transactionManager, contractGasProvider, BINARY, "", initialWeiValue);
    }

    @Deprecated
    public static RemoteCall<SendEther> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, BigInteger initialWeiValue) {
        return deployRemoteCall(SendEther.class, web3j, credentials, gasPrice, gasLimit, BINARY, "", initialWeiValue);
    }

    @Deprecated
    public static RemoteCall<SendEther> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, BigInteger initialWeiValue) {
        return deployRemoteCall(SendEther.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "", initialWeiValue);
    }
}
