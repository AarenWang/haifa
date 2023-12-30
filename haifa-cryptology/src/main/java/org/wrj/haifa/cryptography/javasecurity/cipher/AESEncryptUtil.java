package org.wrj.haifa.cryptography.javasecurity.cipher;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
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

    //static final String TRANSFORMATION = "AES";

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
        kg.init(256);
        // 生成密钥
        SecretKey secretKey = kg.generateKey();
        // 获取二进制密钥编码形式
        byte[] bytes = secretKey.getEncoded();
        System.out.println("++++++++++++++++++++++++++++public key++++++++++++++++++++++++++++++");
        String hex = new String(Hex.encodeHex(bytes,false));
        System.out.printf("hex="+hex);
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
            SecretKeySpec key = new SecretKeySpec(Base64.decodeBase64(secretKey), ALGORITHM);
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

    public static void main(String[] args) throws UnsupportedEncodingException,Exception {
        String key = genKeyPair();
        System.out.println("base64 key="+key);
        Long timestmap = System.currentTimeMillis();
        String str = timestmap+"_"+DEVICE_ID;
        byte[] b = encrypt(str.getBytes("UTF-8"),key);
        byte[] nb = decrypt(b, str);
        try {
            System.out.println(new String(nb, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }

    }


    private static final String DEVICE_ID = "eyJraWQiOiJRWFl6dkZpXC9FK0xQM2RsUlh3dXZOdjhURHNlYUxiTVdQNUVMRCtxd2Vndz0iLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiIyYmNiNDViOC0wYmIxLTQ1ZGUtYjAyZS1jZWM0N2E0MjQ5Y2UiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiY3VzdG9tOnNpZ25fdXBfdHlwZSI6IjEiLCJpc3MiOiJodHRwczpcL1wvY29nbml0by1pZHAuYXAtc291dGhlYXN0LTEuYW1hem9uYXdzLmNvbVwvYXAtc291dGhlYXN0LTFfNGRxUVdOTU4yIiwiY29nbml0bzp1c2VybmFtZSI6ImVjaG9vbzAwMSIsImN1c3RvbTpzaWduX3VwX25hbWUiOiJ5YW5namluZ2p1bkB2YWxsZXlzb3VuZC54eXoiLCJjdXN0b206Y3JlYXRlX3RpbWUiOiIxNjc1MTM0NzE5Iiwib3JpZ2luX2p0aSI6ImI5NzY1N2RkLTM5ODgtNDAwNi05ZjJhLTIwODg1M2JlODY1YyIsImF1ZCI6IjF2Z2J0dTdhMGlyOG5saXBobWVmZ2FzNGI5IiwiZXZlbnRfaWQiOiIzNGMzNGJjNS04YzdkLTRjYzQtOGIxNi1kNWZkY2E0NTg4MDgiLCJ0b2tlbl91c2UiOiJpZCIsImN1c3RvbTppbml0X3Bhc3N3b3JkIjoiMSIsImF1dGhfdGltZSI6MTY5MDQ0NTQ1MiwiY3VzdG9tOnN0YXR1cyI6IjEiLCJuaWNrbmFtZSI6InlhbmdqaW5nanVuQHZhbGxleXNvdW5kLnh5eiIsImV4cCI6MTY5MDUzMTg1MiwiY3VzdG9tOmhhc19wYXlfcGFzc3dvcmQiOiIxIiwiaWF0IjoxNjkwNDQ1NDUyLCJqdGkiOiI5OTliNDY3Yi1lNTdhLTQwZTktYWNkZS02YjUwMDhjZDg5MDAiLCJlbWFpbCI6InlhbmdqaW5nanVuQHZhbGxleXNvdW5kLnh5eiJ9.YiuLu2fbwqCBOOSl3tUr4DAqzDSd37r2BA137wt4P86pxClHr4X5FZkwWH3Ocl5cN0JXYJ8f25w6u_PkSGz5cafaf4D-tJpLZcWqTWZErUwrqa2dv_Nck38jb8u_DayBZ8EBOjLHV8Kmz_1vujhe0jN4C2gXgKjGEqDFqGsv3nXan5hGzi6sTrO8rrMUv-ja8AWCgU_tHZL_YfmItZE88XvpdwBD44Py3VdEpHd-0ctsMGknoCyIB2UHofh6l0T23HDXYqWpbaEc1NDtw9UlfAkY8nTNAmCEO5-m1MYAHQiWJjbG2sgWpThEkQ7SwAYhBIb6pRSywi75YOSZ0OtbQw";
}
