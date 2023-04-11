package com.nextgen.gameaggregator;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceDto;
import com.nextgen.gameaggregator.operator.wallet.bet.WalletBetDto;
import com.nextgen.gameaggregator.util.ApiSecurityUtils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    void testSignatureWalletBet() {
        Gson gson = new Gson();
        WalletBetDto walletBetDto = new WalletBetDto();

        walletBetDto.setTraceId("fc6112c7-fe4e-4453-ae88-18a7e8b62979");
        walletBetDto.setUsername("rwqrqwe");
        walletBetDto.setTransactionId("c01512a2-a19b-4973-862c-d0883b68345b");
        walletBetDto.setExternalTransactionId("fa228a98-84c8-4e4b-bf0a-bde06470c9a2");
        walletBetDto.setAmount(BigDecimal.valueOf(5));
        walletBetDto.setCurrency("CNY");
        walletBetDto.setToken("ecf9d4df-f0a5-48e7-b773-ada7040cf632");
        walletBetDto.setGameCode("PP_bjmb");
        walletBetDto.setRoundId("48867e2d-7ce1-442f-aeef-8bd1a1f2c8da");
        walletBetDto.setTimestamp(1681176476596L);


        String jsonPayload = gson.toJson(walletBetDto);
        System.out.println(jsonPayload);
        String apiSecret = "319f90ab0762831a0fb00f99f3f8572b18c3ae9ff14df6144a9f7998a37eb198";
        String actualSignature = ApiSecurityUtils.getHmacSignature(jsonPayload, apiSecret);
        System.err.println(actualSignature);
    }

    @Test
    void testSignatureWalletBalance() {
        Gson gson = new Gson();
        WalletBalanceDto walletBalanceDto = new WalletBalanceDto();

        walletBalanceDto.setTraceId("4fce7194-b507-492a-a757-723f643c130a");
        walletBalanceDto.setUsername("testsignature");
        walletBalanceDto.setCurrency("CNY");
        walletBalanceDto.setToken("af766113-2ce7-4827-b746-8ef056efbafd");

        String jsonPayload = gson.toJson(walletBalanceDto);

        System.out.println(jsonPayload);
        String apiSecret = "9632b4a7a57fcdfb8339b7dc2e57dae3778216378810155f9f74c057cb99921b";
        String actualSignature = ApiSecurityUtils.getHmacSignature(jsonPayload, apiSecret);
        System.err.println(actualSignature);
    }
}
