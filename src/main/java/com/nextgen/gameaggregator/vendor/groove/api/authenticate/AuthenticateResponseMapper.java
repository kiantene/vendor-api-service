package com.nextgen.gameaggregator.vendor.groove.api.authenticate;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.groove.constant.GameMode;
import com.nextgen.gameaggregator.vendor.groove.constant.OrderType;
import com.nextgen.gameaggregator.vendor.groove.constant.ResponseCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.nextgen.gameaggregator.vendor.groove.util.VendorUtil.formatBalance;

@Component
public class AuthenticateResponseMapper implements AuthenticateVendorResponseMapper<AuthenticateResponse> {
    @Override
    public AuthenticateResponse toVendor(AuthenticateContext context, PlayerBalanceData balanceData) {
        return AuthenticateResponse.builder()
                .accountid(context.getVendorPlayerUsername())
                .city("Jakarta")
                .code(ResponseCode.SUCCESS.code)
                .country("IN")
                .currency(context.getVendorCurrency())
                .gamesessionid(context.getVendorSessionToken())
                .real_balance(formatBalance(balanceData.getBalance()))
                .bonus_balance(formatBalance(BigDecimal.ZERO))
                .status(ResponseCode.SUCCESS.message)
                .game_mode(GameMode.REAL_MONEY.value)
                .order(OrderType.CASH_MONEY.value)
                .build();
    }
}
