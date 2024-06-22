package me.aarenwang.web3j;

import org.web3j.crypto.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class Wallet {
    public static void main(String[] args) throws CipherException, IOException {
        if(args.length < 2){
            System.out.println("please set mnemonic and password");
            return;
        }

        String password = args[0];
        String mnemonic = args[1];
        boolean validate =MnemonicUtils.validateMnemonic(mnemonic);
        System.out.println(validate);


      load(args);
      generateBip39WalletFromMnemonic(args);

    }

    /**
     * 通过keystore文件加载钱包
     * @param args
     * @throws CipherException
     * @throws IOException
     */
    private static void load(String[] args) throws CipherException, IOException {
        String userHomeDir = System.getProperty("user.home");
        File credentialsFile = Paths.get(userHomeDir,"web3j","11.json").toAbsolutePath().toFile();

        String password = args[0];
        Credentials credentials = WalletUtils.loadCredentials(password,credentialsFile);
        System.out.println(credentials.getAddress()+":"+credentials.getEcKeyPair().getPublicKey());
    }


    /**
     * 通过助记词生成钱包
     * @param args
     * @throws CipherException
     * @throws IOException
     */
    private static void generateBip39WalletFromMnemonic(String[] args) throws CipherException, IOException {

        String password = args[0];
        String mnemonic = args[1];
        boolean validate =MnemonicUtils.validateMnemonic(mnemonic);
        System.out.println(validate);


        byte[]  entropy = MnemonicUtils.generateEntropy(mnemonic);
        ECKeyPair pair = ECKeyPair.create(entropy);
        System.out.printf("privateKey=%d,publicKey=%d \n",pair.getPrivateKey(),pair.getPublicKey());

        String userHomeDir = System.getProperty("user.home");
        File web3jDir = Paths.get(userHomeDir,"web3j").toAbsolutePath().toFile();  //目录为HOME/web3j
        Bip39Wallet bip39Wallet = WalletUtils.generateBip39WalletFromMnemonic(password,mnemonic,web3jDir);
        System.out.println(bip39Wallet.getMnemonic());



    }
}
