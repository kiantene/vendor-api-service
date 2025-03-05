package com.nextgen.gameaggregator.vendor.kypoker.service;

import com.nextgen.gameaggregator.exception.InvalidEncryptionException;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.vendor.kypoker.api.settle.SettleDto;
import lombok.Data;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;


import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
@Service
public class VendorService extends BaseVendorService {

    public static String aesEncrypt(String dataString, String appKey) throws InvalidEncryptionException {
        try {
            // Ensure AES-128-ECB with PKCS7 padding
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

            // Ensure appKey is exactly 16 bytes for AES-128
            byte[] raw = Arrays.copyOf(appKey.getBytes("UTF-8"), 16);  // Pad/truncate to 16 bytes
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");

            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

            byte[] encrypted = cipher.doFinal(dataString.getBytes("UTF-8"));

            // Base64 encoding
            String base64 = java.util.Base64.getEncoder().encodeToString(encrypted);

            // URL encoding
            return base64;
        }catch (Exception exception) {
            throw new InvalidEncryptionException();
        }
    }

    public static String AESDecrypt(String value,String key,boolean isDecodeURL) throws Exception {
        try {
            byte[] raw = key.getBytes("UTF-8");
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            if(isDecodeURL)	value = URLDecoder.decode(value, "UTF-8");
            byte[] encrypted1 = Base64.decode(value);// 先用base64解密
            byte[] original = cipher.doFinal(encrypted1);
            String originalString = new String(original, "UTF-8");
            return originalString;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static String MD5Encrypt(String sourceStr) {
        String result = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(sourceStr.getBytes("UTF-8"));
            byte b[] = md.digest();
            int i;
            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            result = buf.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    private ResultType getResultType(SettleDto dto) {

        ResultType resultType = ResultType.BET_LOSE;
        BigDecimal zero = BigDecimal.ZERO;

        if (dto.getWinAmount().compareTo(zero) > 0 || dto.getJackpotAmount().compareTo(zero) > 0) {
            resultType = ResultType.BET_WIN;
        }

        return resultType;
    }


}
