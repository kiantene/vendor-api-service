package com.nextgen.gameaggregator.vendor.jdb.api.credit;

import com.nextgen.gameaggregator.core.engine.wallet.credit.CreditContext;
import com.nextgen.gameaggregator.core.engine.wallet.credit.CreditContextMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Translates a JDB Action 10 (settle — "Settle Bet and Update Balance") wire DTO into the
 * platform-internal {@link CreditContext} with {@code isRefund = false}.
 * <p>
 * Wire-to-platform name translation:
 * <ul>
 *   <li>{@code transferId} → {@code vendorTxnId} + {@code idempotencyKey}</li>
 *   <li>{@code refTransferIds} → {@code referencedVendorDebitTxnIds}</li>
 *   <li>{@code gameRoundSeqNo} → {@code vendorRoundId}</li>
 *   <li>{@code amount} (payout) → {@code vendorAmount} (the wallet movement)</li>
 *   <li>{@code bet} (original wager) → {@code vendorBetAmount} (analytics passthrough)</li>
 *   <li>{@code win} → {@code vendorWinAmount}</li>
 *   <li>{@code netWin} → {@code vendorWinLoss} (System vocabulary — matches operator wire)</li>
 *   <li>{@code validBet} → {@code vendorEffectiveTurnover} (System vocabulary — matches operator wire)</li>
 * </ul>
 *
 * <p>JDB-specific wire fields that never reach the operator/BetHistory ({@code tax},
 * {@code commission}, {@code historyId}) are intentionally not propagated to the context.
 * If a future need arises, route them through a vendor-private side-channel — not the
 * shared framework context.
 */
@Component
public class JdbSettleContextMapper implements CreditContextMapper<SettleActionDto> {

    @Override
    public CreditContext toInternal(SettleActionDto request) {
        return CreditContext.builder()
                .isRefund(false)
                .idempotencyKey(request.getTransferId())
                .vendorTxnId(request.getTransferId())
                .vendorRoundId(request.getRoundId())
                .vendorAmount(request.getAmount())                  // JDB's `amount` = payout = wallet movement
                .vendorBetAmount(request.getBet())                  // JDB's `bet` = original wager (analytics)
                .vendorTimestamp(request.getTs())
                .referencedVendorDebitTxnIds(toStringList(request.getRefTransferIds()))
                .vendorWinAmount(request.getWin())
                .vendorWinLoss(request.getNetWin())
                .vendorEffectiveTurnover(request.getValidBet())
                .vendorBetTime(request.getVendorBetTime())
                .vendorSettleTime(request.getVendorSettleTime())
                .isRoundEnded(request.getRoundClosed())
                .vendorPlayerUsername(request.getUid())
                .vendorCurrency(request.getCurrency())
                .vendorGameCode(request.getVendorGameCode())
                .build();
    }

    private static List<String> toStringList(List<Long> longs) {
        if (longs == null || longs.isEmpty()) return List.of();
        return longs.stream().map(String::valueOf).toList();
    }
}
