package com.nextgen.gameaggregator.core.engine.wallet.credit;

import com.nextgen.gameaggregator.core.engine.wallet.idempotency.WalletIdempotencyResolver;
import com.nextgen.gameaggregator.entity.couchbase.CreditTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.service.data.CreditTxnDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Credit-side specialisation of {@link WalletIdempotencyResolver}. Builds a fresh
 * {@link CreditTxn} (state = SETTLED for settle mode, REFUNDED for refund mode)
 * and delegates the switch logic to the parent.
 */
@Service
@RequiredArgsConstructor
public class CreditIdempotencyResolver extends WalletIdempotencyResolver<CreditContext, CreditTxn> {

    private final CreditTxnDataService creditTxnDataService;

    @Override
    protected CreditTxn buildFreshTxn(CreditContext context, long now) {
        GameRoundState targetState = context.isRefund() ? GameRoundState.REFUNDED : GameRoundState.SETTLED;
        CreditTxn fresh = CreditTxn.of(context.getVendorClassName(), context.getVendorTxnId(), targetState, now);
        populateAudit(fresh, context, now);
        return fresh;
    }

    @Override
    protected Optional<KvDoc<CreditTxn>> insertOrGet(CreditTxn fresh) {
        return creditTxnDataService.insertOrGet(fresh);
    }

    /** Wire fields only — see {@link WalletIdempotencyResolver#docId}. */
    @Override
    protected String docId(CreditContext context) {
        return CreditTxn.createDocId(context.getVendorClassName(), context.getVendorTxnId());
    }

    @Override
    protected KvDoc<CreditTxn> findById(String docId) {
        return creditTxnDataService.findById(docId);
    }

    @Override
    protected String entityKind() {
        return "Credit";
    }

    private void populateAudit(CreditTxn credit, CreditContext context, long now) {
        credit.setVendorId(context.getVendorId());
        credit.setVendorRoundId(context.getVendorRoundId());
        credit.setVendorPlayerUsername(context.getVendorPlayerUsername());
        // Vendor-facing code, not the platform VendorGame.code — see DebitIdempotencyResolver.
        credit.setVendorGameCode(context.getVendorGameCode());
        credit.setAgentCurrency(context.getCurrencyCode());
        credit.setVendorAmount(context.getVendorAmount());
        credit.setVendorBetAmount(context.getVendorBetAmount());
        credit.setVendorBetTime(context.getVendorBetTime() != null ? context.getVendorBetTime() : now);

        if (context.isRefund()) {
            credit.setReferencedVendorDebitTxnIds(context.getReferencedVendorDebitTxnIds());
        } else {
            credit.setVendorWinAmount(context.getVendorWinAmount());
            credit.setVendorWinLoss(context.resolveVendorWinLoss());
            credit.setVendorEffectiveTurnover(context.resolveVendorEffectiveTurnover());
            credit.setVendorJackpotAmount(context.getVendorJackpotAmount());
            credit.setVendorSettleTime(context.getVendorSettleTime());
            credit.setIsRoundEnded(context.getIsRoundEnded());
        }
    }
}
