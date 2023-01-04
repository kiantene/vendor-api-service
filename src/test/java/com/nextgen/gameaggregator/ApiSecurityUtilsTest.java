package com.nextgen.gameaggregator;

import com.nextgen.gameaggregator.util.ApiSecurityUtils;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class ApiSecurityUtilsTest {
    @Test
    void testKeyGeneration() {
        try {
            Map<String, String> keypair = ApiSecurityUtils.generateKeyPair();
            String publicKey = keypair.get(ApiSecurityUtils.PUBLIC_KEY_SHA256);
            String privateKey = keypair.get(ApiSecurityUtils.PRIVATE_KEY_SHA256);

            System.out.println("========== Base64 encoding ==========");
            System.out.println("Public  Key: " + keypair.get(ApiSecurityUtils.PUBLIC_KEY));
            System.out.println("Private Key: " + keypair.get(ApiSecurityUtils.PRIVATE_KEY));

            System.out.println("\n========== Base64-SHA256 ==========");
            System.out.println("Public  Key: " + publicKey);
            System.out.println("Private Key: " + privateKey);

            System.out.println("\n========== HMAC-SHA256 ==========");

            String data = "{\"user\":\"3nYTOSjdlF6UTz9Ir\",\"country\":\"XX\",\"currency\":\"EUR\",\"operator_id\":1,\"token\":\"cd6bd8560f3bb8f84325152101adeb45\",\"platform\":\"GPL_DESKTOP\",\"game_code\":\"clt_dragonrising\",\"lang\":\"en\",\"lobby_url\":\"https://examplecasino.io\",\"ip\":\"::ffff:10.0.0.39\"}";
            String signature = ApiSecurityUtils.getHmacSignature(data, privateKey);
            System.out.println("Signature  : " + signature + " (" + signature.length() + ")");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
    }

    @Test
    void testKeyExchange() {
        try {
            Map<String, String> aliceKeypair = ApiSecurityUtils.generateKeyPair();
            Map<String, String> bobKeypair = ApiSecurityUtils.generateKeyPair();

            System.out.println("========== Alice's Key Pair ==========");
            String alicePublicKey = aliceKeypair.get(ApiSecurityUtils.PUBLIC_KEY);
            String alicePrivateKey = aliceKeypair.get(ApiSecurityUtils.PRIVATE_KEY);
            System.out.println("Public  Key 1: " + alicePublicKey);
            System.out.println("Private Key 1: " + alicePrivateKey);

            System.out.println("\n========== Bob's Key Pair ==========");
            String bobPublicKey = bobKeypair.get(ApiSecurityUtils.PUBLIC_KEY);
            String bobPrivateKey = bobKeypair.get(ApiSecurityUtils.PRIVATE_KEY);
            System.out.println("Public  Key 2: " + bobPublicKey);
            System.out.println("Private Key 2: " + bobPrivateKey);

            System.out.println("\n========== Shared Secret ==========");
            String aliceSharedSecret = ApiSecurityUtils.generateSharedSecret(alicePrivateKey, bobPublicKey);
            String bobSharedSecret = ApiSecurityUtils.generateSharedSecret(bobPrivateKey, alicePublicKey);
            System.out.println("Alice Shared Secret: " + aliceSharedSecret);
            System.out.println("Bob   Shared Secret: " + bobSharedSecret);
            System.out.println("\n========== END ==========");

        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
