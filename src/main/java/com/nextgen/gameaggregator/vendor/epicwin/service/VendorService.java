package com.nextgen.gameaggregator.vendor.epicwin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParseException;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.InvalidSignatureException;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.epicwin.api.endround.SettleDto;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@Data
public class VendorService extends BaseVendorService {

    public static Long convertDateTimeStringToTimestamp(String dateTimeString, String dateTimeFormat, ZoneId zoneId) {

        if (Objects.isNull(dateTimeString) || Objects.isNull(dateTimeFormat) || Objects.isNull(zoneId))
            return System.currentTimeMillis();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateTimeFormat);
        ZonedDateTime zonedDateTime = LocalDateTime.parse(dateTimeString, formatter).atZone(zoneId);
        return zonedDateTime.toInstant().toEpochMilli();
    }

    public static void isSameSignature(String sign, String toVerifySign) throws InvalidSignatureException {
        if (!sign.equals(toVerifySign)) throw new InvalidSignatureException();
    }

//    public static String generateSign(String functionName, String requestDateTime, String operatorId, String secretKey, String playerId) {
//
//        // Concatenate the values
//        String stringToHash = functionName + requestDateTime + operatorId + secretKey + playerId;
//
//        // Generate the MD5 hash
//        String hashValue = generateMD5Hash(stringToHash);
//
//        return hashValue;
//    }

    public static String generateSign(String data) {
        // Generate the MD5 hash
        String hashValue = generateMD5Hash(data);

        return hashValue;
    }

    public static String generateMD5Hash(String input) {
        String md5Hash = null;

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes());
            StringBuilder result = new StringBuilder();

            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }

            md5Hash = result.toString();
        } catch (Exception e) {
            md5Hash = null;
        }

        return md5Hash;

    }

    public static String convertMapToJson(MultiValueMap<String, String> dataMap) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(dataMap.toSingleValueMap());
        } catch (Exception e) {
            return null;
        }
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

    @Override
    public SettledBet updateSettleBetDataBeforeInsertToKafka(SettledBet settledBet, String rawData) {
        // Get the JSON request body from the HttpRequestLog
        String requestBody = rawData;

        try {
            // Convert the JSON request body to SettleDto object
            SettleDto dto = HttpService.convertJsonToDto(requestBody, SettleDto.class);

            // Remap externalTransactionId with resultId
            settledBet.setExternalTransactionId(String.valueOf(dto.getResultId()));


        } catch (JsonParseException | JsonProcessingException e) {
            log.error("Error parsing JSON: " + e.getMessage());
        }

        return settledBet;
    }

}
