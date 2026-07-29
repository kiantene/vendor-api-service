package com.nextgen.gameaggregator.operator.wallet.rollback;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Extra rollback hints sent ONLY to the internal transfer wallet (ga-seamless-transfer-api).
 *
 * <p>Operator-facing rollbacks intentionally carry no amount — the operator decides its own
 * reversal. The internal transfer wallet, however, used to read settled_bet / unsettled_bet to
 * learn the amount to reverse; the new framework no longer persists those collections, so we
 * pass the amounts inline instead.</p>
 *
 * <p>Per the GA-14599 contract the reversal is expressed as two amounts: {@code betAmount} (stake to
 * add back) and {@code winAmount} (winnings to deduct back, jackpot folded in). Both are OPERATOR-POV
 * (the values actually posted to the wallet, already converted), so the consumer reverses the
 * exact posted figure with no re-conversion.</p>
 */
@Data
public class RollbackMeta {
    /** Stake to add back (operator currency). */
    private BigDecimal betAmount;
    /** Winnings to deduct back, including jackpot (operator currency). */
    private BigDecimal winAmount;
}
