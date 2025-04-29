package me.aarenwang.web3j.tools;

import org.web3j.crypto.*;

import java.io.File;
import java.security.SecureRandom;

public class MnemonicWalletGenerator {

    public static void main(String[] args) throws Exception {
        // 1. 生成 128 位随机 entropy（用于生成 12 个助记词）
        byte[] initialEntropy = new byte[16];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(initialEntropy);

        // 2. 生成助记词（返回 String）
        String mnemonic = MnemonicUtils.generateMnemonic(initialEntropy);
        System.out.println("助记词: " + mnemonic);

        // 3. 使用助记词生成种子
        String passphrase = ""; // 通常留空
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, passphrase);

        // 4. 从种子生成 HD 根密钥
        Bip32ECKeyPair masterKeypair = Bip32ECKeyPair.generateKeyPair(seed);

        // 5. 推导 Ethereum 路径：m/44'/60'/0'/0/0
        final int[] derivationPath = {44 | HARDENED_BIT, 60 | HARDENED_BIT, 0 | HARDENED_BIT, 0, 0};
        Bip32ECKeyPair derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeypair, derivationPath);

        // 6. 创建 Credentials（包含地址和密钥对）
        Credentials credentials = Credentials.create(derivedKeyPair);

        System.out.println("私钥: " + credentials.getEcKeyPair().getPrivateKey().toString(16));
        System.out.println("公钥: " + credentials.getEcKeyPair().getPublicKey().toString(16));
        System.out.println("地址: " + credentials.getAddress());

        // 7. 生成钱包文件
        String password = "yourStrongPassword";
        String keyStorePath = "./";
        System.out.println("keystore: " + keyStorePath);
        File keyStore =  new File(keyStorePath);
        if(!keyStore.exists()){
            keyStore.createNewFile();
        }
        String keystoreJson = WalletUtils.generateWalletFile(password, credentials.getEcKeyPair(), keyStore, false);
        System.out.println("Keystore 文件已保存: " + keystoreJson);

    }

    private static final int HARDENED_BIT = 0x80000000;
}


