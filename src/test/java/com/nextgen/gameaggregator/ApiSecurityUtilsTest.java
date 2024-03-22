package com.nextgen.gameaggregator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.operator.wallet.bet.WalletBetDto;
import com.nextgen.gameaggregator.service.HttpService;
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

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    void testSignatureWalletBet() throws JsonProcessingException {
        Gson gson = new Gson();
//        WalletBetDto walletBetDto = new WalletBetDto();

        String test = "{\"traceId\":\"f76fefaa-3dc3-4348-a8a8-e106cf72d00e\",\"username\":\"testbbbbbbb\",\"transactionId\":\"f76fefaa-3dc3-4348-a8a8-e106cf72d00e\",\"externalTransactionId\":\"6434cd93cb15585ac3bc5b74\",\"amount\":15,\"currency\":\"CNY\",\"token\":\"d5d9bb51-1e5a-4189-aed5-372c60825c32\",\"gameCode\":\"PP_cs5triple8gold\",\"roundId\":\"2916168397\",\"timestamp\":1681182099989}";
        WalletBetDto walletBetDto = HttpService.convertJsonToDto(test, WalletBetDto.class);


        String jsonPayload = gson.toJson(walletBetDto);
        System.err.println(jsonPayload);
        String apiSecret = "9632b4a7a57fcdfb8339b7dc2e57dae3778216378810155f9f74c057cb99921b";
        String actualSignature = ApiSecurityUtils.getHmacSignature(jsonPayload, apiSecret);
        System.err.println(actualSignature);
    }

    @Test
    void testSignatureWalletBalance() {
//        Gson gson = new Gson();
//        WalletBalanceDto walletBalanceDto = new WalletBalanceDto();
//
//        walletBalanceDto.setTraceId("4fce7194-b507-492a-a757-723f643c130a");
//        walletBalanceDto.setUsername("testsignature");
//        walletBalanceDto.setCurrency("CNY");
//        walletBalanceDto.setToken("af766113-2ce7-4827-b746-8ef056efbafd");

       // String jsonPayload = "{\"traceId\":\"{{traceId}}\",\"referenceId\":\"{{traceId}}\",\"username\":\"aabbcc\",\"currency\":\"CNY\",\"transferAmount\":0.01}";
//System.err.println(jsonPayload);
//        System.out.println(jsonPayload);
//        String apiSecret = "5053edc990e1a386fef606cd7ee5ac4a6e5f2fda6d56b81ef05422dc58d40068";
//        String actualSignature = ApiSecurityUtils.getHmacSignature(jsonPayload, apiSecret);
//        System.err.println(actualSignature);
    }
}
