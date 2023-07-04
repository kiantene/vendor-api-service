package com.nextgen.gameaggregator.vendor.evoplay.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.BetInformation;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@Data
public class VendorService extends BaseVendorService {
    public static MultiValueMap<String, String> flattenMapIntoMultiValueMap(Map<String, ?> obj, String prefix) {
        MultiValueMap<String, String> flattened = new LinkedMultiValueMap<>();

        for (Map.Entry<String, ?> entry : obj.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String flattenedKey = prefix != null && !prefix.isEmpty() ? prefix + "[" + key + "]" : key;

            if (value instanceof Map) {
                MultiValueMap<String, String> nestedFlattened = flattenMapIntoMultiValueMap((Map<String, ?>) value, flattenedKey);
                flattened.addAll(nestedFlattened);
            } else {
                flattened.add(flattenedKey, value.toString());
            }
        }
        return flattened;
    }

    public static String buildSignature(MultiValueMap<String, String> mapData, String SignatureKey) {
        StringBuilder sb = new StringBuilder();
        for (String key : mapData.keySet()) {
            List<String> values = mapData.get(key);
            if (key.contains("[") || key.contains("]")) {
                sb.append(values.get(0));
                sb.append(":");
            } else {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ':') {
                    sb.setCharAt(sb.length() - 1, '*');
                }
                sb.append(values.get(0));
                sb.append("*");
            }
        }
        return sb.deleteCharAt(sb.length() - 1).append("*").append(SignatureKey).toString();
    }

    public static String md5(String input) {
        return DigestUtils.md5Hex(input);
    }

    public static <O, C> C convertObjectToMap(O object, Type type) {
        Gson gson = new Gson();
        String json = gson.toJson(object);
        return gson.fromJson(json, type);
    }

    public static <T> T convertBodyToDto(String queryString, Type type) {
        Map<String, Object> resultMap = new LinkedHashMap<>();
        queryString = URLDecoder.decode(queryString, StandardCharsets.UTF_8);

        String[] params = queryString.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];
                populateNestedMap(resultMap, key, value);
            }
        }
        return new ModelMapper().map(resultMap, type);
    }

    private static void populateNestedMap(Map<String, Object> resultMap, String key, String value) {
        String[] keys = key.split("\\[|\\]");
        Map<String, Object> currentMap = resultMap;
        for (int i = 0; i < keys.length - 1; i++) {
            String nestedKey = keys[i];
            if (!currentMap.containsKey(nestedKey)) {
                currentMap.put(nestedKey, new LinkedHashMap<>());
            }
            currentMap = (Map<String, Object>) currentMap.get(nestedKey);
        }
        currentMap.put(keys[keys.length - 1], value);
    }

    public static Map<String, Object> rearrangeMap(Map<String, Object> originalMap) {
        String[] specificKeys = {"project", "version"};
        Map<String, Object> rearrangedMap = new LinkedHashMap<>();

        for (String key : specificKeys) {
            if (originalMap.containsKey(key)) {
                rearrangedMap.put(key, originalMap.get(key));
                originalMap.remove(key);
            }
        }

        rearrangedMap.putAll(originalMap);

        return rearrangedMap;
    }

    public static <O> String generateSignature(O Object, String key) {
        Map<String, Object> mapData = rearrangeMap(convertObjectToMap(Object, LinkedHashMap.class));
        mapData.remove("signature");
        Map<String, Object> innerMap = (Map<String, Object>) mapData.get("data");
        if (innerMap != null) {
            innerMap.remove("detailsDto");
        }
        MultiValueMap<String, String> formData = flattenMapIntoMultiValueMap(mapData, "");
        return md5(buildSignature(formData, key));
    }

    public static Long generateTimestamp() {
        return Instant.now().toEpochMilli();
    }

    @Override
    public BigDecimal calculateEffectiveTurnover(BetInformation betInfo) {
        return betInfo.getEffectiveTurnover();
    }

    public ResultType calculateResultType(BetStatus betStatus, BigDecimal winAmount, BigDecimal jackpotAmount, boolean isBet) {

        winAmount = Optional.ofNullable(winAmount).orElse(BigDecimal.ZERO);
        jackpotAmount = Optional.ofNullable(jackpotAmount).orElse(BigDecimal.ZERO);

        boolean isWinAmountMoreThanZero = winAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean isJackpotAmountMoreThanZero = jackpotAmount.compareTo(BigDecimal.ZERO) > 0;

        ResultType resultType = null;

        if (isBet) {
            resultType = ResultType.BET_LOSE;
        } else {
            if (betStatus.equals(BetStatus.UNSETTLED)) {
                resultType = ResultType.LOSE;
            } else {
                resultType = ResultType.END;
            }
        }

        if (isWinAmountMoreThanZero || isJackpotAmountMoreThanZero) {
            resultType = (isBet) ? ResultType.BET_WIN : ResultType.WIN;
        }

        return resultType;
    }
}
