package com.nextgen.gameaggregator.vendor.facai.service;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidDecryptionException;
import com.nextgen.gameaggregator.exception.InvalidEncryptionException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.SettledBetService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    @Autowired
    SettledBetService settledBetService;

    public String aesEncrypt(String dataString, String appKey) throws InvalidEncryptionException {
        try {
            Base64.Encoder encoder = Base64.getEncoder();
            SecretKeySpec keySpec = new SecretKeySpec(appKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return encoder.encodeToString(cipher.doFinal(dataString.getBytes("UTF-8")));
        } catch (Exception exception) {
            throw new InvalidEncryptionException();
        }
    }

    public String aesDecrypt(String dataString, String appKey, HttpRequestLog httpRequestLog, String body) throws InvalidDecryptionException {
        try {
            Base64.Decoder decoder = Base64.getDecoder();
            SecretKeySpec keySpec = new SecretKeySpec(appKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            //Add decrypt value into request body
            String jsonParam = new String(cipher.doFinal(decoder.decode(dataString)));
            httpRequestLog.setRequestBody(body + ", Decrypt Value:" + jsonParam);
            return jsonParam;
        } catch (Exception exception) {
            throw new InvalidDecryptionException();
        }
    }

    public static String md5(String input) throws InvalidEncryptionException {
        try {
            return DigestUtils.md5Hex(input);
        } catch (Exception exception) {
            throw new InvalidEncryptionException();
        }
    }

    public boolean isValidDateString(String timestamp, String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        try {
            Date date = dateFormat.parse(timestamp);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public SettledBet couchBaseCheckSettledRecord(Long vendorPlayerId, String externalBetId) {
        SettledBet checkRecord = null;

        try {
            checkRecord = settledBetService.getByVendorPlayerIdAndExternalTransactionId(vendorPlayerId, externalBetId);
        } catch (BetNotFoundException e) {
            return null; //if record not found then return null;
        }

        return checkRecord;
    }
}
