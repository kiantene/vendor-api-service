package com.nextgen.gameaggregator.vendor.megafair.api.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.megafair.constant.Formats;
import com.nextgen.gameaggregator.vendor.megafair.constant.Results;
import com.nextgen.gameaggregator.vendor.megafair.response.CommonResponse;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class BetResultResponseMapper implements BetResultVendorResponseMapper<CommonResponse> {
    private static final DateTimeFormatter VENDOR_DATE_FORMATTER =
            DateTimeFormatter.ofPattern(Formats.DATE_TIME_FORMAT).withLocale(Locale.ENGLISH);

    @Override
    public CommonResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        return CommonResponse.builder()
                .time(VENDOR_DATE_FORMATTER.format(ZonedDateTime.now()))
                .response(
                        CommonResponse.ResponseData.builder()
                                .result(Results.SUCCESS)
                                .extSystemTransactionId(context.getTransactionId())
                                .balance(balanceData.getBalance())
                                .build()
                )
                .build();
    }
}
