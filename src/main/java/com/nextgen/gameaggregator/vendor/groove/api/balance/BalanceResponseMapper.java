package com.nextgen.gameaggregator.vendor.groove.api.balance;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.groove.constant.GameMode;
import com.nextgen.gameaggregator.vendor.groove.constant.OrderType;
import com.nextgen.gameaggregator.vendor.groove.constant.ResponseCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.nextgen.gameaggregator.vendor.groove.util.VendorUtil.formatBalance;

@Component
public class BalanceResponseMapper implements BalanceVendorResponseMapper<BalanceResponse> {
    @Override
    public BalanceResponse toVendor(BalanceContext context, PlayerBalanceData balanceData) {
        return BalanceResponse.builder()
                .balance(formatBalance(balanceData.getBalance()))
                .bonus_balance(formatBalance(BigDecimal.ZERO))
                .code(ResponseCode.SUCCESS.code)
                .real_balance(formatBalance(balanceData.getBalance()))
                .status(ResponseCode.SUCCESS.message)
                .game_mode(GameMode.REAL_MONEY.value)
                .order(OrderType.CASH_MONEY.value)
                .build();
    }
}
