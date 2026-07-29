package com.nextgen.gameaggregator.service.business.maxpayout;

import java.math.BigDecimal;

/**
 * Result of applying the agent max-payout cap. All amounts are in <b>vendor units</b>
 * (same space as the input {@link CapRequest}); the operator/platform-currency view is
 * produced later at emit time via {@code fromVendorRate}.
 *
 * <p>The capped amounts are always populated — when no cap applies they equal the input
 * amounts and {@link #capped()} is {@code false}. Callers should read {@link #cappedWin()} /
 * {@link #cappedJackpot()} unconditionally and use {@link #capped()} only to decide whether to
 * emit the separate uncapped bet-history record.
 *
 * <p>{@code winLoss = win - bet} (jackpot excluded), matching the codebase convention.
 * NOTE: {@link #cappedWinLoss()} is only meaningful when {@link CapRequest#betAmount()} is the
 * stake on the same txn (legacy adapter / BET_N_RESULT). For a SettleByRound RESULT the bet is a
 * separate txn (betAmount null → treated as 0), so {@code cappedWinLoss} is not consumed there —
 * the round-level win_loss is derived at aggregation from the BET + RESULT slices.
 */
public record ResultAmounts(
        BigDecimal cappedWin,
        BigDecimal cappedJackpot,
        BigDecimal cappedWinLoss,
        boolean capped) {

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** No cap applied — capped amounts mirror the raw input. */
    public static ResultAmounts uncapped(BigDecimal bet, BigDecimal win, BigDecimal jackpot) {
        return new ResultAmounts(nz(win), nz(jackpot), nz(win).subtract(nz(bet)), false);
    }
}
