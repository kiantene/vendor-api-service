package com.nextgen.gameaggregator.vendor.playtech.service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.playtech.constant.Format;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
public class VendorService extends BaseVendorService {

    private final WalletService walletService;

    @Autowired
    private VendorService(WalletService walletService) {
        this.walletService = walletService;
    }

    public static String returnTime() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.ofHours(8));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Format.DATE_TIME_FORMAT);
        String formattedDateTime = now.format(formatter);
        formattedDateTime = formattedDateTime.replace("Z", "+08:00");

        return formattedDateTime;
    }

    public static String convertBetOrSettleTime(long time) {
        ZonedDateTime dateTime = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Format.DATE_TIME_FORMAT);
        String formattedDateTime = dateTime.format(formatter);

        return formattedDateTime.replace("Z", "+08:00");
    }

    public static Long convertStringToMillis(String datetimeString) {
        try {
            // Use Instant.parse to directly convert the datetime string to an Instant
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Format.DATE_TIME_FORMAT);
            LocalDateTime dateTime = LocalDateTime.parse(datetimeString, formatter);
            // Get the Unix timestamp in seconds
            return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            // Get the current system timestamp
            Instant currentTimestamp = Instant.now();

            // Convert the Instant to a Unix timestamp in seconds
            return currentTimestamp.toEpochMilli();
        }
    }

    public void verifyTokenStatus(Integer status) throws AuthenticationException {
        if (!Objects.equals(status, Status.ACTIVE.code)) {
            throw new AuthenticationException("Token status is not active");
        }
    }

    public String removePrefix(String input, String prefix) {
        if (input != null && input.startsWith(prefix)) {
            return input.substring(prefix.length());
        }
        return input;
    }

    public BigDecimal getCurrentBalance(String traceId, GameSession gameSession, HttpRequestLog httpRequestLog) {
        try {
            return walletService.getBalance(traceId, gameSession, httpRequestLog);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    public String getExtractToken(String token) {
        return token.substring(token.lastIndexOf("_") + 1);
    }
}