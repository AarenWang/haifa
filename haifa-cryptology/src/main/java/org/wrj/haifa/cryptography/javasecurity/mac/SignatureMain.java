package org.wrj.haifa.cryptography.javasecurity.mac;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class SignatureMain {

    private static final String HMAC_ALGO = "HmacSHA256";

    private static final String API_SECRET = "your_api_secret";

    public static void main(String[] args) {
        String data = "pid=141";
        String signature = getSignature(data,API_SECRET);
        System.out.println("signature = " + signature);
        isValidSignature(data,API_SECRET,signature);
        System.out.println("isValidSignature = " + isValidSignature(data,API_SECRET,signature));

    }



    private static boolean isValidSignature(String data, String key, String signature) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), HMAC_ALGO);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes());
            String expectedSignature = Base64.getEncoder().encodeToString(rawHmac);
            return expectedSignature.equals(signature);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String getSignature(String data, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), HMAC_ALGO);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes());
            String signature = Base64.getEncoder().encodeToString(rawHmac);
            return signature;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
