package com.nextgen.gameaggregator.vendor.jdb.api.debit;

import com.nextgen.gameaggregator.core.engine.wallet.debit.DebitContext;
import com.nextgen.gameaggregator.core.engine.wallet.debit.DebitContextMapper;
import org.springframework.stereotype.Component;

/**
 * Translates a JDB Action 9 (debit — "Deduct Balance for Game Room Entry") wire DTO
 * into the platform-internal {@link DebitContext}. JDB wire names → platform names:
 * <ul>
 *   <li>{@code transferId} → {@code vendorTxnId} + {@code idempotencyKey}</li>
 *   <li>{@code gameRoundSeqNo} → {@code vendorRoundId}</li>
 *   <li>{@code amount} → {@code vendorAmount}</li>
 *   <li>{@code ts} → {@code vendorTimestamp}</li>
 *   <li>{@code uid} → {@code vendorPlayerUsername}</li>
 *   <li>{@code currency} → {@code vendorCurrency}</li>
 *   <li>{@code gType} + {@code mType} → {@code vendorGameCode} (joined as {@code gType_mType})</li>
 * </ul>
 * {@code systemSessionId} is deliberately NOT mapped to {@code token} — it is JDB's session id,
 * not a GA-issued one; see {@link DebitActionDto#getSystemSessionId()}. The session is resolved by
 * {@code vendorPlayerUsername}, and {@code DebitContextEnricher} backfills the operator token from
 * the resolved GameSession.
 */
@Component
public class JdbDebitContextMapper implements DebitContextMapper<DebitActionDto> {

    @Override
    public DebitContext toInternal(DebitActionDto request) {
        return DebitContext.builder()
                .idempotencyKey(request.getTransferId())
                .vendorTxnId(request.getTransferId())
                .vendorRoundId(request.getRoundId())
                .vendorAmount(request.getAmount())
                .vendorTimestamp(request.getTs())
                .vendorPlayerUsername(request.getUid())
                .vendorCurrency(request.getCurrency())
                .vendorGameCode(request.getVendorGameCode())
                .build();
    }
}
