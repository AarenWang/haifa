package org.wrj.haifa.javasecurity.sign;

import org.wrj.haifa.javasecurity.EncodeUtil;

import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 签名工具类，目前支持SHA1RSA签名算法 Created by wangrenjun on 2017/6/12.
 */
public class SignUtil {

    public static final String KEY_ALGORITHM  = "RSA";        // 密钥算法
    public static final String SIGN_ALGORITHM = "SHA1WithRSA";// 签名算法：NONEwithRSA,MD2withRSA,SHA1WithRSA,SHA256withRSA,SHA384withRSA,SHA512withRSA

    /**
     * PKCS8格式私钥是经过BASE64编码，要先Base64解码
     * 
     * @param pkcs8PrivateKey pkcs8格式RSA私钥
     * @return
     */
    public static final String getSourcePrivateKey(String pkcs8PrivateKey) {
        return EncodeUtil.bytesToHexStr(Base64.getDecoder().decode(pkcs8PrivateKey.getBytes()));
    }

    /**
     * @param privateKey 私钥 PKCS8格式私钥
     * @param src 待签名字符串
     * @return
     */
    public static String generateSHA1withRSASigature(String privateKey, String src) {
        try {

            Signature sigEng = Signature.getInstance(SIGN_ALGORITHM);

            byte[] pribyte = EncodeUtil.hexStrToBytes(privateKey.trim());

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pribyte);
            KeyFactory fac = KeyFactory.getInstance(KEY_ALGORITHM);

            RSAPrivateKey rsaPrivate = (RSAPrivateKey) fac.generatePrivate(keySpec);
            sigEng.initSign(rsaPrivate);
            sigEng.update(src.getBytes());

            byte[] signature = sigEng.sign();
            return EncodeUtil.bytesToHexStr(signature);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 验证签名字符串是否合法，本方法使用SHA1withRSA签名算法
     *
     * @param pubKey 验证签名时使用的公钥(16进制编码)
     * @param sign 签名字符串-16进制编码
     * @param src 签名的原字符串
     * @param encoding 参与签名字符串编码
     * @return
     */
    public static boolean verifySHA1withRSASigature(String pubKey, String sign, String src, String encoding) {
        try {
            Signature sigEng = Signature.getInstance("SHA1withRSA");

            byte[] pubbyte = EncodeUtil.hexStrToBytes(pubKey.trim());

            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(pubbyte);
            KeyFactory fac = KeyFactory.getInstance("RSA");
            RSAPublicKey rsaPubKey = (RSAPublicKey) fac.generatePublic(keySpec);

            sigEng.initVerify(rsaPubKey);
            sigEng.update(src.getBytes(encoding));

            byte[] sign1 = EncodeUtil.hexStrToBytes(sign);
            return sigEng.verify(sign1);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
