package com.nextgen.gameaggregator.vendor.egtdigital.api.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import com.nextgen.gameaggregator.vendor.egtdigital.util.Amount;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

@Component
public class BetResultRequestMapper implements BetResultContextMapper<BetResultRequest> {
    @Override
    public BetResultContext toInternal(BetResultRequest request) {
        BigDecimal jackpotTotal = sumJackpotAmount(request);
        BigDecimal balance = Amount.internal(request.getAmount());
        return BetResultContext.builder()
                .idempotencyKey(request.getRoundNumber())
                .vendorBetId(request.getTransferId())
                .roundId(request.getRoundNumber())
                .vendorGameCode(request.getGameKey())
                .vendorSessionToken(request.getSessionId())
                .vendorPlayerUsername(request.getPlayerId())
                .vendorSettleTime(System.currentTimeMillis())
                .isFreeSpin(request.getReason().equals("GIFT_SPIN") ? 1 : 0)
                .jackpotAmount(jackpotTotal)
                .winAmount(balance.subtract(jackpotTotal))
                .roundEnded(Boolean.TRUE)
                .build();
    }

    public BigDecimal sumJackpotAmount(BetResultRequest request) {
        if (request == null || request.getJackPot() == null) {
            return BigDecimal.ZERO;
        }

        return request.getJackPot().stream()
                .map(BetResultRequest.Jackpot::getAmount)
                .filter(Objects::nonNull)
                .map(Amount::internal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
