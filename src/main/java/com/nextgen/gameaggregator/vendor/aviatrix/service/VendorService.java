package com.nextgen.gameaggregator.vendor.aviatrix.service;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.vendor.aviatrix.constant.Format;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class VendorService extends BaseVendorService {

    public static String returnTime() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.ofHours(8));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Format.DATE_TIME_FORMAT);
        String formattedDateTime = now.format(formatter);

        formattedDateTime = formattedDateTime.replace("Z", "+08:00");

        return formattedDateTime;
    }

    public static ResultType calculateResultType(BigDecimal winAmount, BetStatus betStatus) {

        winAmount = Optional.ofNullable(winAmount).orElse(BigDecimal.ZERO);

        boolean isWinAmountMoreThanZero = winAmount.compareTo(BigDecimal.ZERO) > 0;

        ResultType resultType = (betStatus.equals(BetStatus.UNSETTLED)) ? ResultType.LOSE : ResultType.END;

        if (isWinAmountMoreThanZero) {
            resultType = ResultType.WIN;
        }

        return resultType;
    }


    @Override
    public boolean shouldSettleByBet() {
        return true;
    }

}
