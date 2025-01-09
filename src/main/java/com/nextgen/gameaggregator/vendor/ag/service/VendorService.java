package com.nextgen.gameaggregator.vendor.ag.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.ag.vo.CommonVo;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@Getter
@Setter
public class VendorService extends BaseVendorService {

    private WalletService walletService;
    private boolean rejectSettleAfterRollback = true;

    public VendorService(WalletService walletService) {
        this.walletService = walletService;
    }

    public static String buildParamString(Map<String, String> formDataJson) {
        StringBuilder paramBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : formDataJson.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            paramBuilder.append(key).append("=").append(value).append("/\\\\\\\\/");
        }
        String paramString = paramBuilder.toString();
        return paramString.substring(0, paramString.length() - 6);
    }

    public static String encrypt(String data, String desKey) throws InvalidFormatException {
        try {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("DES/ECB/PKCS5Padding");
            javax.crypto.SecretKeyFactory keyFactory = javax.crypto.SecretKeyFactory.getInstance("DES");
            javax.crypto.spec.DESKeySpec keySpec = new javax.crypto.spec.DESKeySpec(desKey.getBytes());
            javax.crypto.SecretKey key = keyFactory.generateSecret(keySpec);

            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
        } catch (Exception e) {
            throw new InvalidFormatException("Encrypt Fail");
        }
    }

    public static String encryptMd5Key(String params, String md5Key) throws InvalidFormatException {
        try {
            String combined = params + md5Key;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(combined.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new InvalidFormatException("Encrypt MD5 Fail");
        }
    }

    public static String generateRandomNumber(String cAgent) {
        SecureRandom random = new SecureRandom();
        int minLength = 13;
        int maxLength = 16;
        int length = minLength + random.nextInt(maxLength - minLength + 1);
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        String randomid = sb.toString();
        return cAgent + randomid;
    }

    public void verifyTokenStatus(Integer status) throws AuthenticationException {
        if (!Objects.equals(status, Status.ACTIVE.code)) {
            throw new AuthenticationException();
        }
    }

    public String generateXmlResponse(CommonVo commonVo) throws JsonProcessingException {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
        return xmlMapper.writeValueAsString(commonVo);
    }

    @Override
    public boolean shouldRejectCancelRequest() {
        //Temporary only BGAMING, SpadeGaming, EvoNetent need to accept cancel request
        return this.rejectSettleAfterRollback;
    }

    public BigDecimal getCurrentBalance(String traceId, GameSession gameSession, HttpRequestLog httpRequestLog) {
        try {
            return walletService.getBalance(traceId, gameSession, httpRequestLog);
        } catch (Exception e) {
            log.error("Failed to get balance for traceId: {}", traceId, e);
            return BigDecimal.ZERO;
        }
    }
}
