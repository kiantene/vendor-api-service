package com.nextgen.gameaggregator.vendor.groove.api.betandresult;

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
public class BetAndResultResponseMapper implements BetResultVendorResponseMapper<BetAndResultResponse> {

    @Override
    public BetAndResultResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        return BetAndResultResponse.builder()
                .balance(formatBalance(balanceData.getBalance()))
                .bonus_balance(formatBalance(BigDecimal.ZERO))
                .bonusmoneybet(formatBalance(BigDecimal.ZERO))
                .bonusWin(formatBalance(BigDecimal.ZERO))
                .real_balance(formatBalance(balanceData.getBalance()))
                .realmoneybet(formatBalance(context.getBetAmount()))
                .realMoneyWin(formatBalance(context.getWinAmount()))
                .walletTx(context.getIdempotencyKey())
                .game_mode(GameMode.REAL_MONEY.value)
                .order(OrderType.CASH_MONEY.value)
                .code(ResponseCode.SUCCESS.code)
                .status(ResponseCode.SUCCESS.message)
                .build();
    }
}
