package com.nextgen.gameaggregator.vendor.ezugi.api.v2.result;

import com.nextgen.gameaggregator.core.engine.wallet.BetTransaction;
import com.nextgen.gameaggregator.vendor.ezugi.constant.VendorBetType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
final class BetTransactionMapper {
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private BetTransactionMapper() {}

    public static List<BetTransaction> mapToBetTransactions(BetResultRequest req) {
        if (req == null || req.getGameDataString() == null) return Collections.emptyList();

        BetResultRequest.GameDataString gds = req.getGameDataString();
        List<BetResultRequest.Bet> bets = gds.getBetsList();
        if (bets == null || bets.isEmpty()) return Collections.emptyList();

        Map<String, BigDecimal> winningBets = gds.getWinningBets();

        return bets.stream()
                .filter(Objects::nonNull)
                .map(b -> toBetTransaction(b, winningBets, req))
                .toList();
    }

    private static BetTransaction toBetTransaction(BetResultRequest.Bet bet,
                                                   Map<String, BigDecimal> winningBets,
                                                   BetResultRequest request) {
        String betName = bet.getBetName();
        BigDecimal betAmount = safe(bet.getBetAmount());
        BigDecimal winAmount = (winningBets == null || betName == null)
                ? ZERO
                : safe(winningBets.get(betName));

        String externalTransactionId = request.getTransactionId() + "-" + betName;

        BetTransaction tx = new BetTransaction();
        tx.setExternalTransactionId(externalTransactionId);
        tx.setVendorBetId(request.getDebitTransactionId());
        tx.setRoundId(request.getDebitTransactionId());
        tx.setBetType(VendorBetType.toInternalBetType(betName).code);
        tx.setBetAmount(betAmount);
        tx.setWinAmount(winAmount);
        tx.setWinLoss(winAmount.subtract(betAmount));
        tx.setEffectiveTurnover(betAmount);
        tx.setIsFreeSpin(0);
        tx.setVendorSettleTime(request.getTimestamp());
        return tx;
    }

    private static BigDecimal safe(BigDecimal v) {
        return v == null ? ZERO : v;
    }
}
