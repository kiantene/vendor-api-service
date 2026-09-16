package com.nextgen.gameaggregator.vendor.casinogate.api.win;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import com.nextgen.gameaggregator.vendor.casinogate.util.CasinoGateUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class WinRequestMapper implements BetResultContextMapper<WinRequest> {
    @Override
    public BetResultContext toInternal(WinRequest request) {
        BigDecimal winAmount = request.getAmount() == null
                ? BigDecimal.ZERO
                : CasinoGateUtils.toMajorUnits(request.getAmount());
        int isFreeSpin = request.getFreeSpinId() != null && !request.getFreeSpinId().isBlank()
                ? 1
                : 0;

        return BetResultContext.builder()
                .idempotencyKey(request.getTransactionId())
                .vendorBetId(request.getBetTransactionId())
                .roundId(request.getRoundId())
                .vendorPlayerUsername(request.getVendorPlayerUsername())
                .token(request.getToken())
                .winAmount(winAmount)
                .isFreeSpin(isFreeSpin)
                .roundEnded(!Boolean.FALSE.equals(request.getEndRound()))
                .vendorSettleTime(System.currentTimeMillis())
                .build();
    }
}
