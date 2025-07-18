package com.nextgen.gameaggregator.vendor.aviatorstudio.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.VendorLineService;
import lombok.Getter;
import lombok.Setter;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Setter
@Getter
public class VendorService extends BaseVendorService {

    private static final String PUBLIC_KEY_STRING = "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----";
    private static final String PRIVATE_KEY_STRING = "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----";
    private static final String JWT_SECRET = "your_jwt_secret_here";
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;

    public VendorService(VendorLineService vendorLineService,
                         GameSessionService gameSessionService) {
        this.vendorLineService = vendorLineService;
        this.gameSessionService = gameSessionService;
    }

    public static String generateJWT(String userId, String sessionId, String jwtToken, String publicKey) throws Exception {
        long issuedAtMillis = System.currentTimeMillis();

        Algorithm algorithm = Algorithm.HMAC256(JWT_SECRET);
        String jwt = JWT.create()
                .withClaim("userId", userId)
                .withClaim("sessionId", sessionId)
                .sign(algorithm);

        PublicKey generatedPublicKey = loadPublicKey(publicKey);

        return encrypt(jwt, generatedPublicKey);
    }

    public static PublicKey loadPublicKey(String keyStr) throws Exception {
        String publicKeyPEM = keyStr
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    public static String encrypt(String message, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(message.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static DecodedJWT decodeJWT(String jwtToken) {
        Algorithm algorithm = Algorithm.HMAC256(JWT_SECRET);
        JWTVerifier verifier = JWT.require(algorithm).build();

        return verifier.verify(jwtToken);
    }

//    public static void main(String[] args) {
//        try {
//            // Create JWT and encrypt it
//            String userId = "test-user-123";
//            String sessionId = "123456";
//            String jwtToken = generateJWT(userId, sessionId, PUBLIC_KEY_STRING);
//            PublicKey publicKey = loadPublicKey(PUBLIC_KEY_STRING);
//            String encryptedToken = encrypt(jwtToken, publicKey);
//
//            System.out.println("Encrypted JWT Token: " + encryptedToken);
//
//            // Simulate provider receiving the encrypted token back
//            PrivateKey privateKey = loadPrivateKey(PRIVATE_KEY_STRING);
//            String decryptedJWT = decrypt(encryptedToken, privateKey);
//
//            System.out.println("Decrypted JWT Token: " + decryptedJWT);
//
//            // Validate JWT token
//            validateJWT(decryptedJWT);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
