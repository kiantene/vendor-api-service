package com.nextgen.gameaggregator.vendor.jdb.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonParseException;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.exception.InvalidDateException;
import com.nextgen.gameaggregator.exception.InvalidDecryptionException;
import com.nextgen.gameaggregator.exception.InvalidEncryptionException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.jdb.api.result.SettleDto;
import com.nextgen.gameaggregator.vendor.jdb.constant.Formats;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    public static String encrypt(String data, String key, String iv) throws InvalidEncryptionException {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            int blockSize = cipher.getBlockSize();
            byte[] dataBytes = data.getBytes("UTF-8");
            int plainTextLength = dataBytes.length;
            if (plainTextLength % blockSize != 0) {
                plainTextLength = plainTextLength + (blockSize - plainTextLength % blockSize);
            }
            byte[] plaintext = new byte[plainTextLength];
            System.arraycopy(dataBytes, 0, plaintext, 0, dataBytes.length);
            SecretKeySpec keyspec = new SecretKeySpec(key.getBytes(), "AES");
            IvParameterSpec ivspec = new IvParameterSpec(iv.getBytes());
            cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
            byte[] encrypted = cipher.doFinal(plaintext);
            return Base64.encodeBase64URLSafeString(encrypted);
        } catch (Exception exception) {
            throw new InvalidEncryptionException(exception.getMessage());
        }
    }

    public static String decrypt(String data, String key, String iv) throws InvalidDecryptionException {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes(), "AES"), new IvParameterSpec(iv.getBytes()));
            String decryptData = new String(cipher.doFinal(Base64.decodeBase64(data)));
            return decryptData;
        } catch (Exception exception) {
            throw new InvalidDecryptionException(exception.getMessage());
        }
    }

    public static Long toTimestamp(String dateString) throws InvalidDateException {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            Date date = dateFormat.parse(dateString);
            long unixTimestamp = date.getTime() / 1000L;
            return unixTimestamp;
        } catch (Exception exception) {
            throw new InvalidDateException(exception.getMessage());
        }
    }

    @Override
    public SettledBet updateSettleBetDataBeforeInsertToKafka(SettledBet settledBet, String rawData) {
        // Get the JSON request body from the HttpRequestLog
        String requestBody = rawData;

        try {

            SettleDto dto = HttpService.convertJsonToDto(requestBody, SettleDto.class);

            // check the settled transaction is JDB Spribe or not
            if (dto.getGType().equals(Formats.SPRIBE)) {

                // Remap vendorBetId & vendorRoundId
                settledBet.setVendorBetId(dto.getGameRoundSeqNo().toString());
                long dateChangeVendorBetId = 1711929600000L; // April 01 2024 GMT + 0
                if (dto.getTs() < dateChangeVendorBetId) {
                    settledBet.setRoundId(dto.getGameSeqNo().toString());
                } else {
                    settledBet.setRoundId(dto.getHistoryId().toString());
                }
            }

        } catch (JsonParseException |
                 JsonProcessingException e) {
            log.error("Error parsing JSON: " + e.getMessage());
        }

        return settledBet;
    }

    public String getCredentialWithKey(VendorLine vendorLine, String key) throws InvalidVendorLineException {
        return vendorLine.getCredentials()
                .stream()
                .filter(vendorLineCredential -> vendorLineCredential.getName().equals(key))
                .map(VendorLineCredential::getValue)
                .findFirst()
                .orElseThrow(InvalidVendorLineException::new);
    }
}
