package me.aarenwang.web3j;


import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.crypto.exception.CipherException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Constant {

    public static final String userHomeDir = System.getProperty("user.home");
    public static final Path CREDENTIALS_FILE_PATH = Paths.get(userHomeDir,"web3j","11.json").toAbsolutePath().toAbsolutePath();

    public static Credentials getCredentialsFromFile(String password) throws CipherException, IOException {
        File credentialsFile = Constant.CREDENTIALS_FILE_PATH.toFile();
        Credentials credentials = WalletUtils.loadCredentials(password,credentialsFile);

        return credentials;
    }

    public static Credentials getCredentialsFromPrivateKey(String privateKey){
        Credentials credentials = Credentials.create(privateKey);
        return credentials;
    }

}
