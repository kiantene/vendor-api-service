package com.nextgen.gameaggregator.vendor.yesbingo.service;

import com.nextgen.gameaggregator.entity.BetInformation;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@Slf4j
@Data
public class VendorService extends BaseVendorService {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;

    public static String encrypt(String str, String key, String iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        byte[] encrypted = cipher.doFinal(padString(str).getBytes(StandardCharsets.UTF_8));
        String encoded = Base64.getEncoder().encodeToString(encrypted);
        String data = encoded.replace("+", "-").replace("/", "_").replace("=", "");

        return data;
    }

    public static String decrypt(String code, String key, String iv) throws Exception {
        code = code.replace('-', '+').replace('_', '/');
        byte[] decodedBytes = Base64.getDecoder().decode(code);
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        String decrypted = new String(decryptedBytes, StandardCharsets.UTF_8).trim();
        return decrypted;
    }

    private static String padString(String str) {
        int blockSize = 16;
        int padSize = blockSize - (str.length() % blockSize);
        char padChar = (char) padSize;
        StringBuilder padded = new StringBuilder(str);
        for (int i = 0; i < padSize; i++) {
            padded.append(padChar);
        }
        return padded.toString();
    }

    public static String getSign(String data) {
        String token = DigestUtils.md5Hex(data);
        return token.toUpperCase();
    }

    @Override
    public BigDecimal calculateEffectiveTurnover(BetInformation betInfo) {
        return betInfo.getEffectiveTurnover();
    }
}
