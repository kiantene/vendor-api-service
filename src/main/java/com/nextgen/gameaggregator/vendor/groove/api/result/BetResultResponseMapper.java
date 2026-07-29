package com.nextgen.gameaggregator.vendor.groove.api.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.groove.constant.GameMode;
import com.nextgen.gameaggregator.vendor.groove.constant.OrderType;
import com.nextgen.gameaggregator.vendor.groove.constant.ResponseCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.nextgen.gameaggregator.vendor.groove.util.VendorUtil.formatBalance;

@Component
public class BetResultResponseMapper implements BetResultVendorResponseMapper<BetResultResponse> {

    @Override
    public BetResultResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        return BetResultResponse.builder()
                .balance(formatBalance(balanceData.getBalance()))
                .bonus_balance(formatBalance(BigDecimal.ZERO))
                .bonusWin(formatBalance(BigDecimal.ZERO))
                .code(ResponseCode.SUCCESS.code)
                .real_balance(formatBalance(balanceData.getBalance()))
                .realMoneyWin(formatBalance(context.getWinAmount()))
                .status(ResponseCode.SUCCESS.message)
                .walletTx(context.getIdempotencyKey())
                .game_mode(GameMode.REAL_MONEY.value)
                .order(OrderType.CASH_MONEY.value)
                .build();
    }
}
