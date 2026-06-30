package com.nextgen.gameaggregator.vendor.groove.api.freeround;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.groove.api.result.BetResultResponse;
import com.nextgen.gameaggregator.vendor.groove.constant.GameMode;
import com.nextgen.gameaggregator.vendor.groove.constant.OrderType;
import com.nextgen.gameaggregator.vendor.groove.constant.ResponseCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.nextgen.gameaggregator.vendor.groove.util.VendorUtil.formatBalance;

@Component
public class FreeRoundPayoutResponseMapper implements PromoPayoutVendorResponseMapper<BetResultResponse> {

    @Override
    public BetResultResponse toVendor(PromoPayoutContext context, PlayerBalanceData balanceData) {
        return BetResultResponse.builder()
                .balance(formatBalance(balanceData.getBalance()))
                .bonus_balance(formatBalance(balanceData.getBalance()))
                .bonusWin(formatBalance(context.getVendorPayoutAmount()))
                .code(ResponseCode.SUCCESS.code)
                .real_balance(formatBalance(BigDecimal.ZERO))
                .realMoneyWin(formatBalance(BigDecimal.ZERO))
                .status(ResponseCode.SUCCESS.message)
                .walletTx(context.getIdempotencyKey())
                .game_mode(GameMode.BONUS.value)
                .order(OrderType.BONUS_MONEY.value)
                .build();
    }
}
