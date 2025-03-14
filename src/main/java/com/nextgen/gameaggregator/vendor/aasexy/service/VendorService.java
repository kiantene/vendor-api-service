package com.nextgen.gameaggregator.vendor.aasexy.service;

import com.couchbase.client.core.deps.com.fasterxml.jackson.core.JsonProcessingException;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.JsonNode;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.node.ObjectNode;
import com.nextgen.gameaggregator.entity.ga.BetNotFoundLog;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.DuplicateExternalTransactionIdException;
import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.BetNotFoundLogService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.aasexy.vo.BalanceVo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service("aasexyVendorService")
@Setter
@Getter
public class VendorService extends BaseVendorService {
    @Autowired
    private BetNotFoundLogService betNotFoundLogService;
    @Autowired
    private WalletService walletService;
    public static List<BalanceVo> processMultipleDataResponds(List<CompletableFuture<BalanceVo>> balanceVo) {

        // set every completable future 5 sec timeout, if timeout then return null
        List<CompletableFuture<BalanceVo>> betsWithTimeout = balanceVo.stream()
                .map(bet -> bet.orTimeout(5L, TimeUnit.SECONDS)
                        .exceptionally(ex -> null))  // 如果超时，返回 null
                .toList();

        // use allOf to wait all CompletableFuture complete
        CompletableFuture<Void> allBets = CompletableFuture.allOf(betsWithTimeout.toArray(new CompletableFuture[0]));
        allBets.join();  // 等待所有任务完成

        // collect all result（include null）

        return betsWithTimeout.stream()
                .map(CompletableFuture::join)  // get every CompletableFuture result
                .collect(Collectors.toList());
    }

    public static Long getTimeStamp(String datetimeString) {
        try {
            // Use Instant.parse to directly convert the datetime string to an Instant
            Instant instant = Instant.parse(datetimeString);

            // Get the Unix timestamp in seconds
            return instant.toEpochMilli();
        } catch (Exception e) {
            // Get the current system timestamp
            Instant currentTimestamp = Instant.now();

            // Convert the Instant to a Unix timestamp in seconds
            return currentTimestamp.toEpochMilli();
        }
    }

    public void processMultipleDataResponse(List<CompletableFuture<BigDecimal>> balances) {

        // set every completable future 5 sec timeout, if timeout then return null
        List<CompletableFuture<BigDecimal>> betsWithTimeout = balances.stream()
                .map(bet -> bet.orTimeout(5L, TimeUnit.SECONDS)
                        .exceptionally(ex -> null))  // 如果超时，返回 null
                .toList();

        // use allOf to wait all CompletableFuture complete
        CompletableFuture<Void> allBets = CompletableFuture.allOf(betsWithTimeout.toArray(new CompletableFuture[0]));
        allBets.join();  // 等待所有任务完成
    }

    public String convertDateTimeFormat(Long unixTimestamp) {

        // Convert milliseconds to Instant
        Instant instant = Instant.ofEpochMilli(unixTimestamp);

        // Define the GMT-4 time zone
        ZoneId gmtMinus4 = ZoneId.of("GMT+8");

        // Convert Instant to ZonedDateTime with the GMT-4 time zone
        ZonedDateTime zonedDateTime = instant.atZone(gmtMinus4);

        // Define the format for ISO8601
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        // Format the ZonedDateTime to ISO8601 format

        return zonedDateTime.format(formatter);
    }

    public String convertBodyToJson(String body) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode jsonBody = objectMapper.createObjectNode();

        // Check if the body is not empty
        if (body != null && !body.isEmpty()) {
            // Convert the string body to JSON
            String[] parts = body.split("&");
            for (String part : parts) {
                String[] keyValue = part.split("=", 2); // Limit split to 2 parts
                if (keyValue.length == 2) {
                    String key = keyValue[0];
                    String value = keyValue[1];

                    // If the key is "message", parse its value as JSON
                    if ("message".equals(key)) {
                        // Parse the JSON string value into a JsonNode
                        JsonNode jsonValue = objectMapper.readTree(value);
                        // Add the parsed JSON value to the jsonBody
                        jsonBody.set(key, jsonValue);
                    } else {
                        jsonBody.put(key, value);
                    }
                }
            }
        }

        // Convert ObjectNode to JSON string
        return jsonBody.toString();
    }

    public ResultType calculateResultType(BigDecimal betAmount, BigDecimal winAmount, BigDecimal jackpotAmount, String settleType) {

        winAmount = Optional.ofNullable(winAmount).orElse(BigDecimal.ZERO);
        jackpotAmount = Optional.ofNullable(jackpotAmount).orElse(BigDecimal.ZERO);

        boolean isWinAmountMoreThanZero = winAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean isJackpotAmountMoreThanZero = jackpotAmount.compareTo(BigDecimal.ZERO) > 0;

        ResultType resultType = (!settleType.equals("platformTxId")) ? ResultType.BET_LOSE : ResultType.END;

        if (isWinAmountMoreThanZero || isJackpotAmountMoreThanZero) {
            resultType = (!settleType.equals("platformTxId")) ? ResultType.BET_WIN : ResultType.WIN;
        }
        return resultType;
    }

    @Override
    public boolean shouldRejectCancelRequest() {
        //Temporary only AASexy, BGAMING, SpadeGaming, EvoNetent need to accept cancel request
        return false;
    }

    public BigDecimal checkResponseAndReturnBalance(List<CompletableFuture<BalanceVo>> balanceVoList) throws InsufficientBalanceException {
        List<BalanceVo> resultList = processMultipleDataResponds(balanceVoList);

        for (BalanceVo balanceVo : resultList) {
            if (balanceVo == null) {
                return null;
            }else if (balanceVo.getBalance().compareTo(BigDecimal.ZERO) < 0){
                throw new InsufficientBalanceException();
            }
        }
        // Find the latest process bet event
        BalanceVo lastestBalanceVo = resultList.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparing(BalanceVo::getTimestamp))
                .orElse(null);

        if (lastestBalanceVo != null){
            return lastestBalanceVo.getBalance();
        }
        return null;
    }

    public void verifyBetAfterRollback(Long vendorPlayerId, String externalTransactionId) throws DuplicateExternalTransactionIdException {
        BetNotFoundLog betNotFoundLog = betNotFoundLogService.getByVendorPlayerIdAndExternalTransactionId(vendorPlayerId, externalTransactionId);
        // if have data mean have call rollback before
        if (betNotFoundLog != null) {
            throw new DuplicateExternalTransactionIdException();
        }
    }

    public BigDecimal getCurrentBalance(String traceId, GameSession gameSession, HttpRequestLog httpRequestLog) {
        BigDecimal balance = BigDecimal.ZERO;
        try {
            balance = walletService.getBalance(traceId, gameSession, httpRequestLog);
        } catch (Exception ignored) {}
        return balance;
    }
}
