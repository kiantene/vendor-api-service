package com.nextgen.gameaggregator.vendor.jdb.api.credit;

import com.nextgen.gameaggregator.core.engine.wallet.credit.CreditContext;
import com.nextgen.gameaggregator.core.engine.wallet.credit.CreditContextMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Translates a JDB Action 11 (cancel / rollback) wire DTO into the platform-internal
 * {@link CreditContext} with {@code isRefund = true}. Only refund-linkage fields
 * are populated; settle-mode analytics stay null.
 */
@Component
public class JdbRefundContextMapper implements CreditContextMapper<RefundActionDto> {

    @Override
    public CreditContext toInternal(RefundActionDto request) {
        return CreditContext.builder()
                .isRefund(true)
                .idempotencyKey(request.getTransferId())
                .vendorTxnId(request.getTransferId())
                .vendorRoundId(request.getGameRoundSeqNo() != null ? request.getGameRoundSeqNo().toString() : null)
                .vendorAmount(request.getAmount())                  // JDB Action 11's `amount` = refund value
                // vendorBetAmount left null for refunds — Action 11 has no separate `bet` field;
                // OperatorCreditRequestMapper falls back to vendorAmount for the operator wire.
                .vendorTimestamp(request.getTs())
                .referencedVendorDebitTxnIds(toStringList(request.getRefTransferIds()))
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
