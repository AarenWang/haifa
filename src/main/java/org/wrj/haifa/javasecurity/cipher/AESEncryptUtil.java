package org.wrj.haifa.javasecurity.cipher;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * <pre>
 *
 * </pre>
 *
 * @author Wayne.Wang<5waynewang@gmail.com>
 * @since 3:00:39 PM Nov 19, 2015
 */
public class AESEncryptUtil {

    static final String ALGORITHM      = "AES";
    static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    static final byte[] IV             = "AES@xiangqu@@com".getBytes();
    static final String PROVIDER       = BouncyCastleProvider.PROVIDER_NAME;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * 随机生成密钥对
     */
    public static String genKeyPair() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance(ALGORITHM);
        // 初始化密钥生成器，AES要求密钥长度为128位、192位、256位
        kg.init(128);
        // 生成密钥
        SecretKey secretKey = kg.generateKey();
        // 获取二进制密钥编码形式
        byte[] bytes = secretKey.getEncoded();
        System.out.println("++++++++++++++++++++++++++++public key++++++++++++++++++++++++++++++");
        System.out.println(Base64.encodeBase64String(bytes));
        return Base64.encodeBase64String(bytes);
    }

    /**
     * 加密
     *
     * @param plaintext 明文数据
     * @return 密文数据
     */
    public static byte[] encrypt(byte[] plaintext, String secretKey) {
        try {
            SecretKeySpec key = new SecretKeySpec(Base64.decodeBase64(secretKey), TRANSFORMATION);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            // 使用CBC模式，需要一个向量iv，可增加加密算法的强度
            IvParameterSpec params = new IvParameterSpec(IV);
            cipher.init(Cipher.ENCRYPT_MODE, key, params);
            return doFinal(cipher, plaintext);
        }
        // catch (NoSuchProviderException e) {
        // throw new RuntimeException("no Provider: " + PROVIDER, e);
        // }
        catch (NoSuchAlgorithmException e) {
            // 无此加密算法
            throw new RuntimeException("invalid algorithm: " + ALGORITHM, e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException("invalid algorithm parameter for " + ALGORITHM, e);
        } catch (NoSuchPaddingException e) {
            // cipher的填充机制 不对
            throw new RuntimeException("no such padding", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("invalid key spec", e);
        }
    }

    static byte[] doFinal(Cipher cipher, byte[] plaintext) {
        try {
            return cipher.doFinal(plaintext);
        } catch (IllegalBlockSizeException e) {
            // 数据长度非法
            throw new RuntimeException("illegal block size", e);
        } catch (BadPaddingException e) {
            // 数据已损坏
            throw new RuntimeException("bad padding", e);
        }
    }

    /**
     * 解密
     *
     * @param ciphertext 密文数据
     * @return 明文数据
     */
    public static byte[] decrypt(byte[] ciphertext, String secretKey) {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(Base64.decodeBase64(secretKey), TRANSFORMATION);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            IvParameterSpec iv = new IvParameterSpec(IV);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            return doFinal(cipher, ciphertext);
        }
        // catch (NoSuchProviderException e) {
        // throw new RuntimeException("no Provider: " + PROVIDER, e);
        // }
        catch (NoSuchAlgorithmException e) {
            // 无此解密算法
            throw new RuntimeException("invalid algorithm: " + ALGORITHM, e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException("invalid algorithm parameter for " + ALGORITHM, e);
        } catch (NoSuchPaddingException e) {
            // cipher的填充机制 不对
            throw new RuntimeException("no such padding", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("invalid key spec", e);
        }
    }

    public static void main(String[] args) {
        byte[] b = encrypt("{\"accountId\":\"epay_test@163.com\"}".getBytes(), "v440a8ziyt/XviQ9nK49Fw==");
        byte[] nb = decrypt(b, "v440a8ziyt/XviQ9nK49Fw==");
        try {
            System.out.println(new String(nb, "UTF-8"));

            genKeyPair();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }


    }
}
