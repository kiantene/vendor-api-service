package com.nextgen.gameaggregator.vendor.aviatorstudio.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.Credentials;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Enumeration;

@Setter
@Getter
@Service
public class VendorService extends BaseVendorService {
    static {
        // Register BouncyCastle as a security provider
        Security.addProvider(new BouncyCastleProvider());
    }

    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;

    public VendorService(VendorLineService vendorLineService,
                         GameSessionService gameSessionService) {
        this.vendorLineService = vendorLineService;
        this.gameSessionService = gameSessionService;
    }

    public static String generateJWT(String userId, String jwtSecret, String publicKey) throws
            InvalidKeySpecException,
            NoSuchAlgorithmException,
            NoSuchPaddingException,
            IllegalBlockSizeException,
            BadPaddingException,
            InvalidKeyException,
            NoSuchProviderException {
        long issuedAtMillis = System.currentTimeMillis();

        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
        String jwt = JWT.create()
                .withClaim("userId", userId)
                .withClaim("iat", issuedAtMillis)
                .sign(algorithm);

        return encrypt(publicKey, jwt);
    }

    public static String encrypt(String publicKeyPEM, String jwtToken) throws
            NoSuchPaddingException,
            NoSuchAlgorithmException,
            NoSuchProviderException,
            InvalidKeySpecException,
            IllegalBlockSizeException,
            BadPaddingException,
            InvalidKeyException {
        // Convert message to bytes
        byte[] messageBytes = jwtToken.getBytes(StandardCharsets.UTF_8);

        // Parse the public key
        PublicKey publicKey = parsePublicKey(publicKeyPEM);

        // Initialize cipher
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        // Encrypt the message
        byte[] encryptedBytes = cipher.doFinal(messageBytes);

        // Encode as base64 string
        return Base64.getEncoder().encodeToString(encryptedBytes);

    }

    private static PublicKey parsePublicKey(String publicKeyPEM)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Remove PEM headers and whitespace
        String publicKeyContent = publicKeyPEM
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        // Decode the base64 content
        byte[] encodedKey = Base64.getDecoder().decode(publicKeyContent);

        // Create the public key
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
        return keyFactory.generatePublic(keySpec);
    }

    public static <T> void doValidation(T validationObject) throws InvalidRequestException {
        ValidationUtils.validateRequest(validationObject);
    }

    public DecodedJWT decodeJWT(String jwtToken, int vendorLineId) throws CredentialNotFoundException {
        String jwtSecret = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.JWT_SECRET);
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);

        return JWT.require(algorithm).build().verify(jwtToken);
    }

    public void verifyJWT(String jwtAuth, int vendorLineId, String vendorPlayerUsername) throws AuthenticationException, CredentialNotFoundException {
        DecodedJWT decodedJWT = decodeJWT(jwtAuth, vendorLineId);

        //Verify username
        ValidationUtils.isEquals(vendorPlayerUsername, decodedJWT.getClaim("userId").asString(), AuthenticationException::new);

        long issuedAtMillis = decodedJWT.getClaim("iat").asLong();
        long nowMillis = System.currentTimeMillis();
        long twoDaysMillis = 2L * 24 * 60 * 60 * 1000;

        if (Math.abs(nowMillis - issuedAtMillis) > twoDaysMillis) {
            throw new AuthenticationException();
        }
    }

    public String getHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        StringBuilder headersString = new StringBuilder();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headersString.append(headerName)
                    .append(":")
                    .append(headerValue)
                    .append("\n");
        }
        return headersString.toString();
    }
}
