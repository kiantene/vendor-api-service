package com.nextgen.gameaggregator.vendor.vplus.api.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import com.nextgen.gameaggregator.vendor.vplus.constant.BetResultType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BetResultRequestMapper implements BetResultContextMapper<BetResultRequest> {
    @Override
    public BetResultContext toInternal(BetResultRequest request) {
        return BetResultContext.builder()
                .idempotencyKey(request.getTransactionId())
                .roundId(request.getGameRoundId())
                .vendorGameCode(String.valueOf(request.getGameId()))
                .vendorPlayerUsername(request.getUsername())
                .winAmount(BetResultType.requiresSettlement(request.getType()) ? request.getBalance() : BigDecimal.ZERO)
                .vendorSettleTime(System.currentTimeMillis())
                .roundEnded(request.getCompleted() == 1)
                .isFreeSpin(request.getFreeSpinning() != null && request.getFreeSpinning() == 1 ? 1 : 0)
                .build();
    }
}
