package com.nextgen.gameaggregator.vendor.kypoker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.vendor.slotegrator.api.rollback.RollbackTransactionDto;
import com.nextgen.gameaggregator.vendor.slotegrator.api.rollback.RollbackTransactionVo;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Data
@Service
public class VendorService extends BaseVendorService {

    public static String HmacSha1Sign(MultiValueMap<String, String> body, MultiValueMap<String, String> header, String secretKey)
            throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        // Combine the two MultiValueMaps into a single map
        Map<String, List<String>> combinedMap = new TreeMap<>();
        combinedMap.putAll(body);
        combinedMap.putAll(header);

        // Build the message by concatenating keys and their URL-encoded values
        StringBuilder messageBuilder = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : combinedMap.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            Collections.sort(values); // Sort values for consistent ordering
            for (String value : values) {
                if (messageBuilder.length() > 0) {
                    messageBuilder.append("&");
                }
                messageBuilder.append(URLEncoder.encode(key, "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(value, "UTF-8"));
            }
        }

        String message = messageBuilder.toString();

        // Generate HMAC-SHA1 signature
        Mac sha1Hmac = Mac.getInstance("HmacSHA1");
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA1");
        sha1Hmac.init(keySpec);

        byte[] hmacBytes = sha1Hmac.doFinal(message.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hmacBytes) {
            sb.append(String.format("%02x", b));
        }
        String temp = sb.toString();
        return sb.toString();
    }

    public static String generateNonce() {
        // Step 1: Generate a unique string (similar to PHP's uniqid(mt_rand(), true))
        String uniqueString = System.currentTimeMillis() + "-" + new Random().nextInt(1000000);

        try {
            // Step 2: Create an MD5 digest of the unique string
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(uniqueString.getBytes());

            // Step 3: Convert the hash bytes to a hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    hexString.append('0'); // Pad with a leading zero if necessary
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException();
        }
    }

    public static List<RollbackTransactionVo> processMultipleDataResponds(List<CompletableFuture<RollbackTransactionVo>> settles) {

        CompletableFuture<Void> allBets = CompletableFuture.allOf(settles.toArray(new CompletableFuture[settles.size()]));
        allBets.join();
        List<RollbackTransactionVo> transactionsList = settles.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        return transactionsList;
    }

    public static <T> T convertQueryStringToDtoUrlDecode(String queryString, Class<T> objectClass) throws InvalidRequestException {
        Map<String, Object> queryParameterMap = new HashMap<>();

        // TODO: To review on this exception handling
        try {
            queryString = URLDecoder.decode(queryString, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        String[] fields = queryString.split("&");

        for (String field : fields) {
            String[] kv = field.split("=");
            if (kv.length == 2) {
                Object currentValue = queryParameterMap.get(kv[0]);
                if (currentValue == null) {
                    queryParameterMap.put(kv[0], kv[1]);
                } else if (currentValue instanceof String) {
                    String[] values = {(String) currentValue, kv[1]};
                    queryParameterMap.put(kv[0], values);
                } else if (currentValue instanceof String[]) {
                    String[] values = (String[]) currentValue;
                    Integer newLength = values.length + 1;
                    String[] newValues = Arrays.copyOf(values, newLength);
                    newValues[newLength - 1] = kv[1];
                    queryParameterMap.put(kv[0], newValues);
                }
            }
        }
        ObjectMapper mapper = new ObjectMapper();

        T object;
        try {
            object = mapper.convertValue(queryParameterMap, objectClass);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException();
        }

        return object;
    }

    @Override
    public boolean shouldRejectCancelRequest() {
        return false;
    }


    public static List<RollbackTransactionDto> processTransactionList(String urlEncodedString) throws UnsupportedEncodingException {
        // Decode the URL-encoded string
        String decodedString = URLDecoder.decode(urlEncodedString, "UTF-8");

        // Convert query parameters into a Map
        Map<String, String> dataMap = Arrays.stream(decodedString.split("&"))
                .map(pair -> pair.split("="))
                .filter(keyValue -> keyValue.length == 2)
                .collect(Collectors.toMap(kv -> kv[0], kv -> kv[1]));

        // Extract rollback transactions dynamically
        List<RollbackTransactionDto> transactions = new ArrayList<>();
        for (int index = 0; ; index++) {
            String baseKey = String.format("rollback_transactions[%d]", index);

            if (!dataMap.containsKey(baseKey + "[action]")) {
                break;
            }

            // Create a new instance for each transaction
            RollbackTransactionDto rollbackTransactionDto = new RollbackTransactionDto();
            rollbackTransactionDto.setAction(dataMap.get(baseKey + "[action]"));
            rollbackTransactionDto.setAmount(new BigDecimal(dataMap.get(baseKey + "[amount]")));
            rollbackTransactionDto.setTransactionId(dataMap.get(baseKey + "[transaction_id]"));
            rollbackTransactionDto.setType(dataMap.get(baseKey + "[type]"));

            transactions.add(rollbackTransactionDto);
        }

        return transactions;
    }
}
