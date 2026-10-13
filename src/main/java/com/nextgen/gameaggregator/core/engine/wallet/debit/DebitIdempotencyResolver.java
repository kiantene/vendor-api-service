package com.nextgen.gameaggregator.core.engine.wallet.debit;

import com.nextgen.gameaggregator.core.engine.wallet.idempotency.WalletIdempotencyResolver;
import com.nextgen.gameaggregator.entity.couchbase.AgentMeta;
import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.service.data.DebitTxnDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Debit-side specialisation of {@link WalletIdempotencyResolver}. Builds a fresh
 * {@link DebitTxn} and delegates the switch logic to the parent.
 */
@Service
@RequiredArgsConstructor
public class DebitIdempotencyResolver extends WalletIdempotencyResolver<DebitContext, DebitTxn> {

    private final DebitTxnDataService debitTxnDataService;

    @Override
    protected DebitTxn buildFreshTxn(DebitContext context, long now) {
        DebitTxn fresh = DebitTxn.of(context.getVendorClassName(), context.getVendorTxnId(), now);
        populateAudit(fresh, context, now);
        return fresh;
    }

    @Override
    protected Optional<KvDoc<DebitTxn>> insertOrGet(DebitTxn fresh) {
        return debitTxnDataService.insertOrGet(fresh);
    }

    /** Wire fields only — see {@link WalletIdempotencyResolver#docId}. */
    @Override
    protected String docId(DebitContext context) {
        return DebitTxn.createDocId(context.getVendorClassName(), context.getVendorTxnId());
    }

    @Override
    protected KvDoc<DebitTxn> findById(String docId) {
        return debitTxnDataService.findById(docId);
    }

    @Override
    protected String entityKind() {
        return "Debit";
    }

    private void populateAudit(DebitTxn debit, DebitContext context, long now) {
        debit.setVendorId(context.getVendorId());
        debit.setVendorRoundId(context.getVendorRoundId());
        debit.setVendorPlayerUsername(context.getVendorPlayerUsername());
        // The vendor-facing code (gType_mType, e.g. "18_18025"), NOT context.getGameCode(),
        // which BaseEnricher sets to the platform VendorGame.code ("JDB_18_18025").
        // Matches how every legacy writer fills its game-code column
        // (BetContextEnricher / BetResultContextEnricher / AdjustmentContextEnricher all use
        // context.getVendorGameCode()), so records written by either framework compare equal.
        debit.setVendorGameCode(context.getVendorGameCode());
        debit.setAgentCurrency(context.getCurrencyCode());
        debit.setVendorAmount(context.getVendorAmount());
        debit.setVendorBetTime(context.getVendorTimestamp() != null ? context.getVendorTimestamp() : now);
        debit.setVendorCurrency(context.getVendorCurrency());
        debit.setAgentMeta(agentMetaOf(context));
    }

    /**
     * Snapshot for the credits that will name this debit. Written with the fresh doc only: an ERROR
     * re-arm keeps the snapshot of the attempt that created it, which is the same player.
     */
    private static AgentMeta agentMetaOf(DebitContext context) {
        AgentMeta agentMeta = new AgentMeta();
        agentMeta.setAgentId(context.getAgentId());
        agentMeta.setUsername(context.getAgentPlayerUsername());
        agentMeta.setCurrency(context.getCurrencyCode());
        agentMeta.setGameCode(context.getGameCode());
        agentMeta.setSession(context.getToken());
        return agentMeta;
    }
}
