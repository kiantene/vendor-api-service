package com.nextgen.gameaggregator.vendor.ezugi.api.v2.result;

import com.nextgen.gameaggregator.core.engine.wallet.BetTransaction;
import com.nextgen.gameaggregator.vendor.ezugi.constant.VendorBetType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class BetTransactionMapper {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private BetTransactionMapper() {
        // Pure utility class - prevent instantiation
    }

    // =========================================================================================================
    // TODO / DEBT: REMOVE AFTER DEPLOYMENT IS STABLE (e.g., after 24-48h retention period for in-flight bets)
    // Temporary parameter: 'resolvedRoundId' is passed from BetResultRequestMapper to support legacy in-flight
    // transactions where roundId was mapped to debitTransactionId.
    // Clean-up target: Remove 'resolvedRoundId' parameter and replace usages with String.valueOf(req.getRoundId()).
    // =========================================================================================================
    public static List<BetTransaction> mapToBetTransactions(BetResultRequest req, String resolvedRoundId) {
        if (req == null || req.getGameDataString() == null) {
            return Collections.emptyList();
        }

        BetResultRequest.GameDataString gds = req.getGameDataString();
        List<BetResultRequest.Bet> bets = gds.getBetsList();
        if (bets == null || bets.isEmpty()) {
            return Collections.emptyList();
        }

        List<BetResultRequest.Bet> finalBets = new ArrayList<>(bets);

        applyCommissionIfExists(finalBets, gds.getBetAmount(), gds.getCommission());

        Map<String, BigDecimal> winningBets = gds.getWinningBets();

        return finalBets.stream()
                .filter(Objects::nonNull)
                .map(b -> toBetTransaction(b, winningBets, req, resolvedRoundId))
                .toList();
    }

    private static BetTransaction toBetTransaction(BetResultRequest.Bet bet,
                                                   Map<String, BigDecimal> winningBets,
                                                   BetResultRequest request,
                                                   String resolvedRoundId) {
        String betName = bet.getBetName();
        BigDecimal betAmount = safe(bet.getBetAmount());
        BigDecimal winAmount = (winningBets == null || betName == null)
                ? ZERO
                : safe(winningBets.get(betName));

        String externalTransactionId = request.getTransactionId() + "-" + betName;

        BetTransaction tx = new BetTransaction();
        tx.setExternalTransactionId(externalTransactionId);
        tx.setVendorBetId(request.getDebitTransactionId());

        // TODO / DEBT: Post-deployment cleanup - change back to String.valueOf(request.getRoundId())
        tx.setRoundId(resolvedRoundId);

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
