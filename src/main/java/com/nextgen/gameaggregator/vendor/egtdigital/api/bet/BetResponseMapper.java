package com.nextgen.gameaggregator.vendor.egtdigital.api.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.egtdigital.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.egtdigital.util.Amount;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BetResponseMapper implements BetVendorResponseMapper<BetResponse> {
    @Override
    public BetResponse toVendor(BetContext context, PlayerBalanceData balanceData) {
        long balance = Amount.vendor(balanceData.getBalance());
        return BetResponse.builder()
                .balance(balance)
                .bonusAmount(Amount.vendor(BigDecimal.ZERO))
                .realAmount(Amount.vendor(context.getBetAmount()))
                .casinoTransferId(context.getVendorBetId())
                .statusCode(ResponseCodes.OK.getCode())
                .build();
    }
}
