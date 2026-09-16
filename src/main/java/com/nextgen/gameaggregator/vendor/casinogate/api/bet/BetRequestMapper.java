package com.nextgen.gameaggregator.vendor.casinogate.api.bet;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import com.nextgen.gameaggregator.vendor.casinogate.util.CasinoGateUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BetRequestMapper implements BetContextMapper<BetRequest> {
    @Override
    public BetContext toInternal(BetRequest request) {
        boolean isFreeSpin = request.getFreeSpinId() != null
                && !request.getFreeSpinId().isBlank();
        BigDecimal betAmount;
        if (isFreeSpin) {
            betAmount = BigDecimal.ZERO;
        } else if (request.getAmount() == null) {
            // CasinoGate in-game free spins can be zero-stake without a freeSpinId.
            betAmount = BigDecimal.ZERO;
        } else {
            betAmount = CasinoGateUtils.toMajorUnits(request.getAmount());
        }

        return BetContext.builder()
                .idempotencyKey(request.getTransactionId())
                .vendorBetId(request.getTransactionId())
                .roundId(request.getRoundId())
                .vendorPlayerUsername(request.getVendorPlayerUsername())
                .token(request.getToken())
                .betAmount(betAmount)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
