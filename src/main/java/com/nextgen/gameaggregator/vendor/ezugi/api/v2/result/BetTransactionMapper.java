package com.nextgen.gameaggregator.vendor.ezugi.api.v2.result;

import com.nextgen.gameaggregator.core.engine.wallet.BetTransaction;
import com.nextgen.gameaggregator.vendor.ezugi.constant.VendorBetType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
final class BetTransactionMapper {
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private BetTransactionMapper() {}

    public static List<BetTransaction> mapToBetTransactions(BetResultRequest req) {
        if (req == null || req.getGameDataString() == null) return Collections.emptyList();

        BetResultRequest.GameDataString gds = req.getGameDataString();
        List<BetResultRequest.Bet> bets = gds.getBetsList();
        if (bets == null || bets.isEmpty()) return Collections.emptyList();

        List<BetResultRequest.Bet> finalBets = new ArrayList<>(bets);
        
        applyCommissionIfExists(finalBets, gds.getBetAmount(), gds.getCommission());

        Map<String, BigDecimal> winningBets = gds.getWinningBets();

        return finalBets.stream()
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

    private static void applyCommissionIfExists(List<BetResultRequest.Bet> finalBets, 
                                                BigDecimal totalBetAmount, 
                                                Integer commission) {
        if (commission != null && commission > 0) {
            BigDecimal commissionAmount = safe(totalBetAmount)
                    .multiply(BigDecimal.valueOf(commission))
                    .divide(BigDecimal.valueOf(100));

            BetResultRequest.Bet commissionBet = new BetResultRequest.Bet();
            commissionBet.setBetName("commission"); 
            commissionBet.setBetAmount(commissionAmount);

            finalBets.add(commissionBet);
        }
    }
}
