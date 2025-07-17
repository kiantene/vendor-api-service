package com.nextgen.gameaggregator.vendor.aviatorstudio.service;

import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.Getter;
import lombok.Setter;

import javax.crypto.Cipher;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Setter
@Getter
public class VendorService extends BaseVendorService {

    public static String encryptMessage(String publicKeyPEM, String message) throws Exception {
        try {
            // Convert message to bytes
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

            // Parse the public key
            PublicKey publicKey = parsePublicKey(publicKeyPEM);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            // Encrypt the message
            byte[] encryptedBytes = cipher.doFinal(messageBytes);

            // Encode as base64 string
            return Base64.getEncoder().encodeToString(encryptedBytes);

        } catch (Exception e) {
            System.err.println("Encryption failed: " + e.getMessage());
            throw e;
        }
    }

    private static PublicKey parsePublicKey(String publicKeyPEM)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
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
}
