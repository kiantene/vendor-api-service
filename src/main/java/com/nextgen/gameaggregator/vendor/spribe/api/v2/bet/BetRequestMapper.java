package com.nextgen.gameaggregator.vendor.spribe.api.v2.bet;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import com.nextgen.gameaggregator.vendor.spribe.utils.AmountConverter;
import org.springframework.stereotype.Component;

@Component
public class BetRequestMapper implements BetContextMapper<BetRequest> {
    @Override
    public BetContext toInternal(BetRequest request) {
        return BetContext.builder()
                .idempotencyKey(request.getProviderTxId())
                .roundId(request.getActionId())
                .vendorGameCode(request.getGame())
                .vendorPlayerUsername(request.getUserId())
                .vendorCurrency(request.getCurrency())
                .betAmount(AmountConverter.convertUnitToBalance(request.getAmount())) // amount is in thousands (no decimals)
                .vendorSessionToken(request.getSessionToken())
                .build();
    }
}
