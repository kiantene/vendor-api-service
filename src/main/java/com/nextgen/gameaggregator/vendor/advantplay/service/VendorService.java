package com.nextgen.gameaggregator.vendor.advantplay.service;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.vendor.advantplay.constant.Formats;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@Slf4j
@Data
public class VendorService extends BaseVendorService {

    public static String validateCredential(String value) throws InvalidVendorLineException {
        return Optional.ofNullable(value)
                .filter(val -> !val.isEmpty()) // Check if the string is non-null and non-empty
                .orElseThrow(InvalidVendorLineException::new); // Set the value using the setter if it is non-null and non-empty
    }

    public static String getTimestamp() {

        // Get the current time with offset
        OffsetDateTime currentTime = OffsetDateTime.now();

        // Define the desired date-time format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Formats.DATE_TIME_FORMAT);

        // Format the current time
        return currentTime.format(formatter);
    }

    public static Long dateTimeConvert(String rawDateTime) {

        //convert date time string to timestamp
        Long timestamp = null;
        if (rawDateTime != null) {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(rawDateTime, DateTimeFormatter.ofPattern(Formats.DATE_TIME_FORMAT));
            timestamp = zonedDateTime.toInstant().toEpochMilli();
        }
        return timestamp;

    }

    private String md5(String input) {
        return DigestUtils.md5Hex(input);
    }

    public String generateHash(String secretKey, String requestBody) {
        return md5(secretKey + requestBody);
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
