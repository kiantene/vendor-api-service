package com.nextgen.gameaggregator.vendor.groove.api.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.groove.constant.GameMode;
import com.nextgen.gameaggregator.vendor.groove.constant.OrderType;
import com.nextgen.gameaggregator.vendor.groove.constant.ResponseCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.nextgen.gameaggregator.vendor.groove.util.VendorUtil.formatBalance;

@Component
public class RollbackResponseMapper implements BetRollbackVendorResponseMapper<RollbackResponse> {

    @Override
    public RollbackResponse toVendor(BetRollbackContext context, PlayerBalanceData balanceData) {
        return RollbackResponse.builder()
                .accounttransactionid(context.getIdempotencyKey())
                .balance(formatBalance(balanceData.getBalance()))
                .bonus_balance(formatBalance(BigDecimal.ZERO))
                .real_balance(formatBalance(balanceData.getBalance()))
                .game_mode(GameMode.REAL_MONEY.value)
                .order(OrderType.CASH_MONEY.value)
                .code(ResponseCode.SUCCESS.code)
                .status(ResponseCode.SUCCESS.message)
                .build();
    }
}
