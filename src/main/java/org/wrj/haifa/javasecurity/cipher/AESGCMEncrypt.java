package org.wrj.haifa.javasecurity.cipher;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Created by wangrenjun on 2017/12/22.
 */
public class AESGCMEncrypt {

    private static final String AES                  = "AES";


    private static final String AES_GCM_NOPADDING = "AES/GCM/NoPadding";

    private static  final  int IV_LENGTH = 32;

    private static final ThreadLocal<Random> rand = new ThreadLocal<Random>() {
        @Override
        protected Random initialValue() {
            Random r = new Random();
            return r;
        }
    };


    private static byte[] genIV() {
        int ivLen = IV_LENGTH;
        if (ivLen == 0)
            return new byte[0];

        // fix for bouncy castle which does accept iv len = 0
        if (ivLen == 1)
            return new byte[]{8};


        byte[] iv = new byte[ivLen];
        rand.get().nextBytes(iv);

        return iv;
    }

    /**
     * @param content 待加密文本
     * @param key 加密密钥
     * @return
     */
    public static byte[] encrypt(String content, String key) {
        try {
            SecretKeySpec keySpec = getKey(key);
            Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);// 创建密码器
            byte[] byteContent = content.getBytes("utf-8");

            IvParameterSpec iv = new IvParameterSpec(genIV());


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
            Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);// 创建密码器

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

    private static SecretKeySpec getKey(String key) throws NoSuchAlgorithmException {

        KeyGenerator kgen = KeyGenerator.getInstance(AES);
        kgen.init(128, new SecureRandom(key.getBytes()));
        SecretKey secretKey = kgen.generateKey();
        byte[] enCodeFormat = secretKey.getEncoded();
        SecretKeySpec keySpec = new SecretKeySpec(enCodeFormat, AES);
        return keySpec;

    }


    public static void main(String[] args) {
        String text = "123232132";
        String key = "2b7e151628aed2a6abf7158809cf4f3c";
        byte[] bytes = encrypt(text, key);

        String newText = decrypt(bytes, key);

        System.out.println(newText);
    }

}
