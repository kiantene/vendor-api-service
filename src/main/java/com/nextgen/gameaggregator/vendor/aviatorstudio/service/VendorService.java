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
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Setter
@Getter
@Service
public class VendorService extends BaseVendorService {
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;

    public VendorService(VendorLineService vendorLineService,
                         GameSessionService gameSessionService) {
        this.vendorLineService = vendorLineService;
        this.gameSessionService = gameSessionService;
    }

    public static String generateJWT(String userId, String sessionId, String jwtToken, String publicKey) throws
            InvalidKeySpecException,
            NoSuchAlgorithmException,
            NoSuchPaddingException,
            IllegalBlockSizeException,
            BadPaddingException,
            InvalidKeyException {
        long issuedAtMillis = System.currentTimeMillis();

        Algorithm algorithm = Algorithm.HMAC256(jwtToken);
        String jwt = JWT.create()
                .withClaim("userId", userId)
                .withClaim("iat", issuedAtMillis)
                .withClaim("sessionId", sessionId)
                .sign(algorithm);

        PublicKey generatedPublicKey = loadPublicKey(publicKey);

        return encrypt(jwt, generatedPublicKey);
    }

    public static PublicKey loadPublicKey(String keyStr) throws
            InvalidKeySpecException,
            NoSuchAlgorithmException,
            NullPointerException {
        String publicKeyPEM = keyStr
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    public static String encrypt(String message, PublicKey publicKey) throws
            NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            IllegalBlockSizeException,
            BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static DecodedJWT decodeJWT(String jwtToken) {
        Algorithm algorithm = Algorithm.HMAC256(jwtToken);
        JWTVerifier verifier = JWT.require(algorithm).build();

        return verifier.verify(jwtToken);
    }

}
