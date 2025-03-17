package org.wrj.haifa.cryptography.javasecurity.keygenrator;

import java.security.SecureRandom;
import java.util.Base64;

public class ApiKeyGenerator {


    private static final int API_KEY_LENGTH = 16; // 32 bytes = 256 bits
    private static final int API_SECRET_LENGTH = 32; // 64 bytes = 512 bits

    public static String generateApiKey() {
        return generateRandomString(API_KEY_LENGTH);
    }

    public static String generateApiSecret() {
        return generateRandomString(API_SECRET_LENGTH);
    }

    private static String generateRandomString(int length) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[length];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public static void main(String[] args) {
        String apiKey = generateApiKey();
        String apiSecret = generateApiSecret();
        System.out.println("API Key: " + apiKey);
        System.out.println("API Secret: " + apiSecret);
    }

}
