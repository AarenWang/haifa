package me.aarenwang.web3j;


import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        Web3j web3 = Web3j.build(new HttpService("http://localhost:8551/"));  // defaults to http://localhost:8545/
        Web3ClientVersion web3ClientVersion = web3.web3ClientVersion().send();
        String clientVersion = web3ClientVersion.getWeb3ClientVersion();
        System.out.println(clientVersion);
    }


}
