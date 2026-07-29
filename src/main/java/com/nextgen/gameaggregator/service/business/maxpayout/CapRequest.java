package com.nextgen.gameaggregator.service.business.maxpayout;

import java.math.BigDecimal;

/**
 * Framework-agnostic input to {@link AgentMaxPayoutService#applyPayoutCap(CapRequest, BigDecimal)}.
 *
 * <p>Carries the identifiers needed to resolve the agent max-payout config plus the raw
 * <b>vendor-unit</b> amounts to cap. Deliberately has no dependency on {@code BetInformation}
 * so the new engine ({@code core.engine.wallet}) can build it straight from a
 * {@code GameTransaction} / {@code GameRound} without dragging the legacy entity across the
 * boundary.
 */
public record CapRequest(
        Integer agentId,
        Integer vendorId,
        Integer gameCategoryId,
        Integer currencyId,
        /**
         * The stake on the SAME txn. Meaningful for the legacy adapter and BET_N_RESULT (bet+result
         * combined). For a SettleByRound RESULT txn the bet is a separate txn so this is null/0 — the
         * cap itself never uses bet, and the caller only reads {@code cappedWin}/{@code cappedJackpot}
         * (round-level win_loss is derived at aggregation, not from {@link ResultAmounts#cappedWinLoss()}).
         */
        BigDecimal betAmount,
        BigDecimal winAmount,
        BigDecimal jackpotAmount) {
}
