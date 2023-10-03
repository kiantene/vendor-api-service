package com.nextgen.gameaggregator.vendor.queenmaker.service;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
@Slf4j
@Data
public class VendorService extends BaseVendorService {

    public static Long convertToTimestamp(String dateTimeString) {
        // Parse the date-time string to an Instant
        Instant instant = Instant.parse(dateTimeString);

        // Convert to milliseconds since the epoch
        return instant.toEpochMilli();
    }

    public static String[] splitGameCode(String vendorGameCode, Integer limit) {
        return vendorGameCode.split("_", limit);
    }

    public static String mergeGameCode(String prefix, String suffix) {
        return prefix + "_" + suffix;
    }

    public ResultType calculateResultType(BigDecimal betAmount, BigDecimal winAmount, BigDecimal jackpotAmount, boolean isBet, BetStatus betStatus) {

        winAmount = Optional.ofNullable(winAmount).orElse(BigDecimal.ZERO);
        jackpotAmount = Optional.ofNullable(jackpotAmount).orElse(BigDecimal.ZERO);

        boolean isWinAmountMoreThanZero = winAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean isJackpotAmountMoreThanZero = jackpotAmount.compareTo(BigDecimal.ZERO) > 0;

        ResultType resultType = (isBet) ? ResultType.BET_LOSE :
                (betStatus == BetStatus.UNSETTLED) ? ResultType.LOSE : ResultType.END;

        if (isWinAmountMoreThanZero || isJackpotAmountMoreThanZero) {
            resultType = (isBet) ? ResultType.BET_WIN : ResultType.WIN;
        }

        return resultType;
    }

}
