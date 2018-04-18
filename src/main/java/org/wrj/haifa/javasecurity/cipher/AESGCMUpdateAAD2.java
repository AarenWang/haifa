package org.wrj.haifa.javasecurity.cipher;

/**
 * Created by wangrenjun on 2018/1/8.
 */
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

public class AESGCMUpdateAAD2 {

    // AES-GCM parameters
    public static final int AES_KEY_SIZE     = 128; // in bits
    public static final int GCM_NONCE_LENGTH = 12;  // in bytes
    public static final int GCM_TAG_LENGTH   = 16;  // in bytes

    public static void main(String[] args) throws Exception {
        int testNum = 0; // pass

        if (args.length > 0) {
            testNum = Integer.parseInt(args[0]);
            if (testNum < 0 || testNum > 3) {
                System.out.println("Usage: java AESGCMUpdateAAD2 [X]");
                System.out.println("X can be 0, 1, 2, 3");
                System.exit(1);
            }
        }
        byte[] input = "Hello AES-GCM World!".getBytes();

        // Initialise random and generate key
        SecureRandom random = SecureRandom.getInstanceStrong();
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_SIZE, random);
        SecretKey key = keyGen.generateKey();

        // Encrypt
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");
        final byte[] nonce = new byte[GCM_NONCE_LENGTH];
        random.nextBytes(nonce);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] aad = "Whatever I like".getBytes();
        ;
        cipher.updateAAD(aad);

        byte[] cipherText = cipher.doFinal(input);

        // Decrypt; nonce is shared implicitly
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        // EXPECTED: Uncommenting this will cause an AEADBadTagException when decrypting
        // because AAD value is altered
        if (testNum == 1) aad[1]++;

        cipher.updateAAD(aad);

        // EXPECTED: Uncommenting this will cause an AEADBadTagException when decrypting
        // because the encrypted data has been altered
        if (testNum == 2) cipherText[10]++;

        // EXPECTED: Uncommenting this will cause an AEADBadTagException when decrypting
        // because the tag has been altered
        if (testNum == 3) cipherText[cipherText.length - 2]++;

        try {
            byte[] plainText = cipher.doFinal(cipherText);
            if (testNum != 0) {
                System.out.println("Test Failed: expected AEADBadTagException not thrown");
            } else {
                // check if the decryption result matches
                if (Arrays.equals(input, plainText)) {
                    System.out.println("Test Passed: match!");
                } else {
                    System.out.println("Test Failed: result mismatch!");
                    System.out.println(new String(plainText));
                }
            }
        } catch (AEADBadTagException ex) {
            if (testNum == 0) {
                System.out.println("Test Failed: unexpected ex " + ex);
                ex.printStackTrace();
            } else {
                System.out.println("Test Passed: expected ex " + ex);
            }
        }
    }
}
