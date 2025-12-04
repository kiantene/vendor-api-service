package com.nextgen.gameaggregator.vendor.ezugi.api.v2.result;

import com.nextgen.gameaggregator.core.engine.wallet.BetTransaction;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class BetResultRequestMapper implements BetResultContextMapper<BetResultRequest> {

    @Override
    public BetResultContext toInternal(BetResultRequest request) {
        BigDecimal betAmount = (request.getGameDataString() != null) ? request.getGameDataString().getBetAmount() : BigDecimal.ZERO;
        BigDecimal winAmount = request.getCreditAmount();
        BigDecimal winloss = winAmount.subtract(betAmount);

        BetResultContext context = BetResultContext.builder()
                .idempotencyKey(request.getTransactionId())
                .vendorPlayerUsername(request.getUid())
                .vendorBetId(request.getDebitTransactionId())
                .roundId(request.getDebitTransactionId())
                .vendorGameCode(String.valueOf(request.getTableId()))
                .vendorCurrency(request.getCurrency())
                .betAmount(BigDecimal.ZERO)
                .winAmount(winAmount)
                .winloss(winloss)
                .effectiveTurnover(betAmount)
                .vendorSettleTime(request.getTimestamp())
                .vendorSessionToken(request.getToken())
                .build();

        List<BetTransaction> betTransactions = BetTransactionMapper.mapToBetTransactions(request);
        context.setBetTransactions(betTransactions);
        return context;
    }
}
