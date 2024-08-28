package com.nextgen.gameaggregator.vendor.db.service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidSignatureException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.UnsettledBetService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.db.api.transfer.TransferDto;
import com.nextgen.gameaggregator.vendor.db.constant.Credentials;
import com.nextgen.gameaggregator.vendor.db.dto.CommonDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    @Autowired
    private UnsettledBetService unsettledBetService;

    public static int generateRandom10DigitNumber() {
        int min = 1000000000;
        int max = 2147483647;
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }

    public static char getRandomChar() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        return chars.charAt(random.nextInt(chars.length()));
    }

    public static String generateRandomString(int n) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < n; i++) {
            stringBuilder.append(getRandomChar());
        }
        return stringBuilder.toString();
    }

    public static String insertRandomString(String md5Hash) {
        int count = 4; // Number of random strings to generate
        String modifiedMD5Hash;
        String[] randomStrings = new String[count];

        // Generate random strings and store them in the array
        for (int i = 0; i < count; i++) {
            randomStrings[i] = generateRandomString(2);
        }

        modifiedMD5Hash =
                randomStrings[0] +
                        md5Hash.substring(0, 9) + randomStrings[1] +
                        md5Hash.substring(9, 17) + randomStrings[2] +
                        md5Hash.substring(17, 32) + randomStrings[3];

        return modifiedMD5Hash;
    }

    public static String decryptToJsonBody(CommonDto commonDto, VendorLineService vendorLineService, String body) throws Exception {

        try {

            //get Credential from database
            Integer vendorLineId = vendorLineService.getVendorLineIdByNameAndValue(Credentials.AGENT, commonDto.getAgent());
            String secretKey = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.SECRET_KEY);
            String iv = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.IV);

            return VendorService.aesDecrypt(body, secretKey, iv);
        } catch (CredentialNotFoundException e) {
            throw new CredentialNotFoundException();
        }

    }

    public static String getEncryptJsonQueryStringBody(String body, String jsonBody, String queryString) {

        return body + ", Decrypted Value: " +
                jsonBody + ", QueryString Value: " + queryString;

    }

    public static String removeSpacesAndBrackets(String input) {
        // Remove spaces
        String withoutSpaces = input.replaceAll("\\s+", "");
        // Remove square brackets
        return withoutSpaces.replaceAll("[\\[\\]]", "");
    }

    public static String md5Generator(String input) {

        String md5Hash;

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = input.getBytes("UTF-8");
            md.update(bytes);

            byte[] digest = md.digest();

            StringBuilder hexString = new StringBuilder(input.length() * 2);

            for (byte b : digest) {
                hexString.append(String.format("%02x", b));
            }

            md5Hash = hexString.toString();
        } catch (Exception e) {
            md5Hash = null;
        }

        return md5Hash;
    }

    public static void verifyHash(String agent, String timestamp, String secretKey, String expectedSign) throws InvalidSignatureException {

        String concatKey = agent + timestamp + secretKey;
        String regenerateSign = md5Generator(concatKey);

        String extractRandomStr = extractCharacters(expectedSign);
        String verifyHash = insertCharacters(regenerateSign, extractRandomStr);
        try {
            ValidationUtils.isEquals(verifyHash, expectedSign, InvalidSignatureException::new);
        } catch (Exception e) {
            throw new InvalidSignatureException();
        }
    }

    public static String extractCharacters(String input) {
        StringBuilder sb = new StringBuilder();

        int[] positions = {0, 1, 11, 12, 21, 22, 38, 39};
        // Iterate through positions and extract characters
        for (int pos : positions) {
            // Check if position is valid
            if (pos < input.length()) {
                char ch = input.charAt(pos);
                sb.append(ch);
            } else {
                sb.append(" "); // Placeholder for invalid positions
            }
        }

        return sb.toString();
    }

    public static String insertCharacters(String input, String toInsert) {
        String segment1 = toInsert.substring(0, 2);
        String segment2 = input.substring(0, 9);
        String segment3 = toInsert.substring(2, 4);
        String segment4 = input.substring(9, 17);
        String segment5 = toInsert.substring(4, 6);

        String segment6 = input.substring(17, 32);
        String segment7 = toInsert.substring(6, 8);
        // Build the transformed string
        StringBuilder sb = new StringBuilder();
        sb.append(segment1).append(segment2).append(segment3)
                .append(segment4).append(segment5)
                .append(segment6).append(segment7);


        return sb.toString();
    }

    public static String encryptAES(String data, String secretKey, String iv) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes());
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("encryptAES error : ", e);
        }
        return null;
    }

    public static String convertToQueryString(MultiValueMap<String, String> multiValueMap) {
        StringBuilder queryString = new StringBuilder();
        for (String key : multiValueMap.keySet()) {
            List<String> values = multiValueMap.get(key);
            for (String value : values) {
                if (!queryString.isEmpty()) {
                    queryString.append("&");
                }
                queryString.append(key).append("=").append(value);
            }
        }
        return queryString.toString();
    }

    public static String aesDecrypt(String encryptedText, String keyStr, String ivStr) throws Exception {
        try {
            byte[] encryptedData = Base64.getDecoder().decode(encryptedText);

            // Convert key and IV strings to bytes
            byte[] keyBytes = keyStr.getBytes();
            byte[] ivBytes = ivStr.getBytes();

            // Create secret key and IV
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            // Perform decryption
            byte[] decryptedData = cipher.doFinal(encryptedData);

            return new String(decryptedData);
        } catch (Exception e) {
            throw new Exception("Error decrypting: " + e.getMessage());
        }
    }

    public boolean searchUnsettledBetForSettle(TransferDto dto, GameSession gameSession) {
        try {
            unsettledBetService.getUnsettledBetByRoundId(dto.getVendorBetId(), dto.getRoundId(), gameSession.getVendorGameId(), gameSession.getVendorPlayerId());

        } catch (BetNotFoundException e) {
            return false;
        }

        return true;
    }

    @Override
    public boolean shouldRejectCancelRequest() {
        return false;
    }
}
