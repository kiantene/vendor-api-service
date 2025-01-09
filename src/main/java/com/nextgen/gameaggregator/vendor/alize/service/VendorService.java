package com.nextgen.gameaggregator.vendor.alize.service;

import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
@Slf4j
public class VendorService extends BaseVendorService {

    private static final String HASH_ALGORITHM = "HmacSHA256";

    public static String generateHash(String apiSecret, String input) {
        try {
            // Create a new secret key based on the given API secret
            SecretKeySpec keySpec = new SecretKeySpec(apiSecret.getBytes(), HASH_ALGORITHM);

            // Generate a message authentication code (MAC) from the input and key
            Mac mac = Mac.getInstance(HASH_ALGORITHM);
            mac.init(keySpec);
            byte[] hashBytes = mac.doFinal(input.getBytes());

            // Convert the MAC to a hexadecimal string format
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating signature : " + e.getMessage());
            throw new RuntimeException("Error generating signature", e);
        }
    }

    @Override
    public boolean shouldRejectCancelRequest() {
        return false;
    }
}
