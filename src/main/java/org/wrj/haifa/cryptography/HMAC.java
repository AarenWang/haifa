package org.wrj.haifa.cryptography;

import sun.misc.BASE64Encoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class HMAC {

    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec keySpec = new SecretKeySpec(
                "qnscAdgRlkIhAUPY44oiexBKtQbGY0orf7OV1I50".getBytes(),
                "HmacSHA1");

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(keySpec);
        byte[] result = mac.doFinal("foo".getBytes());

        BASE64Encoder encoder = new BASE64Encoder();
        System.out.println(encoder.encode(result));
    }
}
