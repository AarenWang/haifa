package org.wrj.haifa.cryptography.javasecurity.cipher;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.*;

/**
 * Created by wangrenjun on 2017/6/6.
 */
public class AESEncrypt {

    private static final String AES                  = "AES";

    private static final String AES_CBC_PKCS5PADDING = "AES/CBC/PKCS5Padding";

    private static final String AES_CBC_NOPADDING = "AES/CBC/NoPadding";


    /**
     * @param content 待加密文本
     * @param key 加密密钥
     * @return
     */
    public static byte[] encrypt(String content, String key) {
        try {
            SecretKeySpec keySpec = getKey(key);
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5PADDING);// 创建密码器
            byte[] byteContent = content.getBytes("utf-8");

            SecureRandom rnd = new SecureRandom();
            IvParameterSpec iv = new IvParameterSpec(rnd.generateSeed(cipher.getBlockSize()));


            cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);// 初始化
            byte[] result = cipher.doFinal(byteContent);
            return result; // 加密

        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
                | BadPaddingException | UnsupportedEncodingException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解密
     *
     * @param content 待解密内容
     * @param key 解密的密钥
     * @return
     */
    public static String decrypt(byte[] content, String key) {
        try {
            SecretKeySpec keySpec = getKey(key);
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5PADDING);// 创建密码器

            SecureRandom rnd = new SecureRandom();
            IvParameterSpec iv = new IvParameterSpec(rnd.generateSeed(cipher.getBlockSize()));

            cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);// 初始化
            byte[] result = cipher.doFinal(content);
            return new String(result);
        } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalBlockSizeException | NoSuchPaddingException
                | BadPaddingException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }

        return null;

    }

    private static SecretKeySpec getKey(String seed) throws NoSuchAlgorithmException {

        KeyGenerator kgen = KeyGenerator.getInstance(AES);
        kgen.init(128, new SecureRandom(seed.getBytes()));
        SecretKey secretKey = kgen.generateKey();
        byte[] enCodeFormat = secretKey.getEncoded();
        SecretKeySpec keySpec = new SecretKeySpec(enCodeFormat, AES);
        return keySpec;

    }

    public static void main(String[] args) throws Exception{
        String seed = "123456";
        for(int i=0; i < 5;i++){
            SecretKeySpec keySpec =  getKey(seed);
            keySpec.getEncoded();
        }

        String text = "123232132";
        String key = "2b7e151628aed2a6abf7158809cf4f3c";

        byte[] bytes = encrypt(text, key);
        String newText = decrypt(bytes, key);

        System.out.println(newText);
    }
}
