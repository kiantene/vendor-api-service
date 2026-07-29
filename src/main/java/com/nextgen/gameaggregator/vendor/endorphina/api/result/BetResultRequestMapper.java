package com.nextgen.gameaggregator.vendor.endorphina.api.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BetResultRequestMapper implements BetResultContextMapper<BetResultRequest> {
    @Override
    public BetResultContext toInternal(BetResultRequest request) {
        return BetResultContext.builder()
                .idempotencyKey(request.getId() != null ? request.getId() : request.getToken() + "_" + request.getPlayer() + "_" + request.getGameId())
                .vendorBetId(request.isProgressive()?null: request.getBetTransactionId()) //if only jackpot will set null,the jackpot will consider settled bet
                .roundId(request.getBetTransactionId())
                .vendorGameCode(request.getGame())
                .vendorPlayerUsername(request.getPlayer())
                .winAmount(request.isProgressive() ? BigDecimal.ZERO : request.getAmount())
                .jackpotAmount(request.isProgressive() ? request.getAmount() : BigDecimal.ZERO)
                .vendorCurrency(request.getCurrency())
                .token(request.getToken())
                .vendorBetTime(request.isProgressive()?request.getDate():null)
                .vendorSettleTime(request.getDate())
                .build();
    }
}
