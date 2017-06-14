package me.wrj.haifa.javascurity.sign;

import me.wrj.haifa.javascurity.EncodeUtil;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Created by wangrenjun on 2017/6/12.
 */
public class SignUtilTest {

    public static void main(String[] args) throws  Exception{
        String priKey = "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAJ78qqQqICeEM+FC"
                        + "R9R+go8Z+HnzFG5qg+a2LEovZsiBncUKLPY4MtjS0/FEUDoYe1pJXLapWti2KgNk"
                        + "mUXNGwHPgjSXGUOXIoZbajmGwoYnEBo4qnZtorlI6Dv6E4+QrzDqe6A+L8y8waoq"
                        + "2Kkwb6jKQTF7ansWLYh16sC+XpODAgMBAAECgYEAkWPGOPI9DWYce3a9cVlv06WQ"
                        + "URU2LfNCRA18WysV925w9OvlShUCir3iC9TI+RfCVkKYgoJFDcEokonAkNCMTJ5k"
                        + "u4wnLP4Qu0l8Z8+EN7XRM1NFPfddMRPSgfgoPBT8/0YChFNjxzKw+WZ1R4Fv9ClZ"
                        + "vV42+0ZadvCvvOyUhAECQQDQapihWsG9Zm9s6ALqwGCxZMsuJxw6ASomcPnJutas"
                        + "fai80zzKICtLZz1A+Zd7vQ91R6WrnCCmddyHoC8fF6uBAkEAw0kMQzwaDC6yZ7OA"
                        + "QCL9Ld6qG4uiQDoir21oyrn98qkmbvPzNQNecBru8NBd5d+hgrS6pQ4pyWllHOP6"
                        + "OLQRAwJBAKPcCyBhQI1uqhBP4Ws70UnnWy9wJGseW0tZ66nFMd7v9OhwlAy+eguQ"
                        + "ocIh+g2ZOTwWFpTz7V+yrq4bLLEfZQECQCrQ/EhNiCR9pI4KFCd7xGjnNgK+Fb/h"
                        + "PzOZWG2CZARtQ8j14bAQ4gbQUO0psjyxUHcY6wc/WLgZTJsN/RGroUMCQQCNfyTh"
                        + "yGQW7y9laLsyjIkj4t4T8ExreZZZPORes34DhyQzQgW4hHsetlgUn2CjwuzM10TI" + "+X6TG9ItDcKseP6g";

        String src = "Hello";
        String sign = SignUtil.generateSHA1withRSASigature(SignUtil.getSourcePrivateKey(priKey), src);
        System.out.printf("src=%s,sign=%s \n", src, sign);

        String publickKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCe/KqkKiAnhDPhQkfUfoKPGfh5"
                            + "8xRuaoPmtixKL2bIgZ3FCiz2ODLY0tPxRFA6GHtaSVy2qVrYtioDZJlFzRsBz4I0"
                            + "lxlDlyKGW2o5hsKGJxAaOKp2baK5SOg7+hOPkK8w6nugPi/MvMGqKtipMG+oykEx"
                            + "e2p7Fi2IderAvl6TgwIDAQAB";

        boolean verifyResult = SignUtil.verifySHA1withRSASigature(SignUtil.getSourcePrivateKey(publickKey), sign, src,
                                                                  "UTF-8");
        System.out.printf("verify result %s \n", verifyResult);


        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
        keyGenerator.initialize(1024);
        KeyPair keyPair = keyGenerator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        System.out.printf("private key =  \n" + EncodeUtil.bytesToHexStr(privateKey.getEncoded()));
        System.out.printf("publicKey key =  \n" + EncodeUtil.bytesToHexStr(publicKey.getEncoded()));

        


    }
}
