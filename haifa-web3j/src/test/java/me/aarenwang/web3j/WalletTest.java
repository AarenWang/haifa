package me.aarenwang.web3j;


import org.junit.Assert;
import org.junit.Test;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.http.HttpService;

public class WalletTest {

    private Web3j web3j = Web3j.build(new HttpService("http://localhost:7545/"));

    @Test
    public void createAddressFromPrivateKey(){
        String privateKey = System.getenv("PRIVATE_KEY");
        Credentials credentials = Credentials.create(privateKey);
        String privateKey1 = credentials.getEcKeyPair().getPrivateKey().toString(16);
        String publicKey = credentials.getEcKeyPair().getPublicKey().toString(16);
        String addr = credentials.getAddress();

        Assert.assertEquals(privateKey,privateKey1);
        Assert.assertEquals("0xB0c4Cf6916eC3064D9d7a1F31931673FC10f984B".toLowerCase(),addr);
    }

    @Test
    public void queryTransaction() throws Exception{
        String hash = "0x58154e7636394f9063cdc0e1317709223e5b87e8a2c1daa6813ff7afe1b8a7e2";
        EthTransaction ethTransaction = web3j.ethGetTransactionByHash(hash).send();
        Transaction tx =ethTransaction.getTransaction().get();
        System.out.println(tx.getFrom());
        System.out.println(tx.getTo());
        System.out.println(tx.getValue());
        System.out.println(tx.getInput());
    }
}
