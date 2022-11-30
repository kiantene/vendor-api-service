package com.nextgen.gameaggregator.vendorapiservice.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

import javax.crypto.KeyAgreement;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class ApiSecurityUtils {
    public static final String KEYPAIR_ALGORITHM = "EC";
    public static final HmacAlgorithms HMAC_ALGORITHM = HmacAlgorithms.HMAC_SHA_256;
    public static final String PUBLIC_KEY = "publicKey";
    public static final String PRIVATE_KEY = "privateKey";
    public static final String PUBLIC_KEY_SHA256 = "publicKeySHA256";
    public static final String PRIVATE_KEY_SHA256 = "privateKeySHA256";

    public static Map<String, String> generateKeyPair() throws NoSuchAlgorithmException {
        // Generate a key pair using ECDSA (Elliptic Curve Digital Signature Algorithm)
        KeyPair keypair = KeyPairGenerator.getInstance(KEYPAIR_ALGORITHM).generateKeyPair();
        HashMap<String, String> keypairMap = new HashMap<>();

        // Obtain the private/public key objects from the key pair
        PrivateKey privateKey = keypair.getPrivate(); // Private key are generated in PKCS#8 format
        PublicKey publicKey = keypair.getPublic();    // Public key are generated in X.509 format

        // Convert both private/public key byte array to base64 readable format
        String privateKeyBase64 = base64Encode(privateKey);
        String publicKeyBase64 = base64Encode(publicKey);

        // Store the keys base64 values into the hashmap
        keypairMap.put(PUBLIC_KEY, publicKeyBase64);
        keypairMap.put(PRIVATE_KEY, privateKeyBase64);

        // Hash the key base64 values using SHA256 and store into the hashmap
        keypairMap.put(PUBLIC_KEY_SHA256, DigestUtils.sha256Hex(publicKeyBase64));
        keypairMap.put(PRIVATE_KEY_SHA256, DigestUtils.sha256Hex(privateKeyBase64));

        return keypairMap;
    }

    public static String getHmacSignature(String data, String key) {
        // Generate HMAC_SHA256 signature for the given data using the provided key
        return new HmacUtils(HMAC_ALGORITHM, key).hmacHex(data);
    }

    public static String generateSharedSecret(String ownPrivateKey, String otherPublicKey) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
        PrivateKey privateKey = base64ToPrivateKey(ownPrivateKey);
        PublicKey publicKey = base64ToPublicKey(otherPublicKey);

        // Create an instance of "Diffie-Hellman" KeyAgreement Object
        // Reference -> https://neilmadden.blog/2016/05/20/ephemeral-elliptic-curve-diffie-hellman-key-agreement-in-java/
        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
        keyAgreement.init(privateKey);
        keyAgreement.doPhase(publicKey, true);
        byte[] sharedSecret = keyAgreement.generateSecret();

        return base64Encode(sharedSecret);
    }

    // To convert a base64 private key into a Java PrivateKey object
    private static PrivateKey base64ToPrivateKey(String base64Key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance(KEYPAIR_ALGORITHM).generatePrivate(keySpec);
    }

    // To convert a base64 public key into a Java PublicKey object
    private static PublicKey base64ToPublicKey(String base64Key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance(KEYPAIR_ALGORITHM).generatePublic(keySpec);
    }

    private static String base64Encode(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    private static String base64Encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
