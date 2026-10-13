package com.nextgen.gameaggregator.vendor.megafair.api.authenticate;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.megafair.constant.Formats;
import com.nextgen.gameaggregator.vendor.megafair.constant.Results;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class AuthenticateResponseMapper implements AuthenticateVendorResponseMapper<AuthenticateResponse> {
    private static final DateTimeFormatter VENDOR_DATE_FORMATTER =
            DateTimeFormatter.ofPattern(Formats.DATE_TIME_FORMAT).withLocale(Locale.ENGLISH);

    @Override
    public AuthenticateResponse toVendor(AuthenticateContext context, PlayerBalanceData balanceData) {
        return AuthenticateResponse.builder()
                .time(VENDOR_DATE_FORMATTER.format(ZonedDateTime.now()))
                .response(
                        AuthenticateResponse.ResponseData.builder()
                                .result(Results.SUCCESS)
                                .userId(context.getVendorPlayerUsername())
                                .currency(context.getVendorCurrency())
                                .balance(balanceData.getBalance())
                                .build()
                )
                .build();
    }
}
