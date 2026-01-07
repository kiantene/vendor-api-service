package com.nextgen.gameaggregator.vendor.gpkv2.api.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import com.nextgen.gameaggregator.vendor.gpkv2.util.TimeStampUtils;
import org.springframework.stereotype.Component;

@Component
public class BetResultRequestMapper implements BetResultContextMapper<BetResultRequest> {
    @Override
    public BetResultContext toInternal(BetResultRequest request) {

        boolean roundEnd = (request.getFinished() != null && request.getFinished()
                && request.getBetTransactionId()==null);

        return BetResultContext.builder()
                .idempotencyKey(request.getTransactionId())
                .vendorBetId(request.getBetTransactionId() == null ? request.getTransactionId() : request.getBetTransactionId())
                .vendorPlayerUsername(request.getOperatorPlayerId())
                .vendorGameCode(request.getGameToken())
                .roundId(request.getRoundId())
                .token(request.getSessionToken())
                .winAmount(request.getAmount())
                .vendorSettleTime(TimeStampUtils.normalizeToMillis(request.getTimestamp()))
                .roundEnded(roundEnd)
                .build();
    }
}