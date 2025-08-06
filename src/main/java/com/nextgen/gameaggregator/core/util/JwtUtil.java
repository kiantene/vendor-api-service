package com.nextgen.gameaggregator.core.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

public class JwtUtil {
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_IAT = "iat";

    static {
        // Register BouncyCastle provider if not already registered
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static String generateJwt(String userId, String jwtSecret, String publicKeyPem)
            throws GeneralSecurityException {
        String jwt = generateSignedJwt(userId, jwtSecret);
        return encryptWithPublicKey(publicKeyPem, jwt);
    }

    private static String generateSignedJwt(String userId, String jwtSecret) {
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
        Instant now = Instant.now();

        return JWT.create()
                .withClaim(CLAIM_USER_ID, userId)
                .withClaim(CLAIM_IAT, System.currentTimeMillis())
//                .withIssuedAt(Date.from(now))
                .sign(algorithm);
    }

    private static String encryptWithPublicKey(String publicKeyPem, String message)
            throws GeneralSecurityException {
        PublicKey publicKey = parsePublicKey(publicKeyPem);

        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] encrypted = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private static PublicKey parsePublicKey(String publicKeyPem) throws GeneralSecurityException {
        String keyContent = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}
