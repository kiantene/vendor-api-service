package com.nextgen.gameaggregator.service.data.producer.transactionhistory;

import com.nextgen.gameaggregator.core.engine.wallet.debit.DebitContext;

/**
 * Adapts a {@link DebitContext} to {@link TransactionHistoryContext} for the
 * {@code BetTransactionHistory} kafka feed. Mirrors
 * {@code BetContextTransactionHistoryAdapter} for the legacy BET path.
 *
 * <p>{@code vendorBetId} is mapped to the vendor's transaction id — a debit isn't a bet,
 * but the kafka consumer expects the field populated with the vendor's primary
 * reference for this wallet movement.
 */
public final class DebitContextTransactionHistoryAdapter implements TransactionHistoryContext {

    private final DebitContext ctx;

    public DebitContextTransactionHistoryAdapter(DebitContext ctx) {
        this.ctx = ctx;
    }

    @Override public String externalTransactionId() { return ctx.getIdempotencyKey(); }
    @Override public String vendorBetId()            { return ctx.getVendorTxnId(); }
    @Override public String roundId()                { return ctx.getVendorRoundId(); }
    @Override public Integer vendorGameId()          { return ctx.getVendorGameId(); }
    @Override public Long vendorPlayerId()           { return ctx.getVendorPlayerId(); }
    @Override public String vendorPlayerUsername()   { return ctx.getVendorPlayerUsername(); }
    @Override public Integer vendorId()              { return ctx.getVendorId(); }
    @Override public Integer vendorLineId()          { return ctx.getVendorLineId(); }
    @Override public Integer agentId()               { return ctx.getAgentId(); }
    @Override public Long agentPlayerId()            { return ctx.getAgentPlayerId(); }
    @Override public String agentPlayerUsername()    { return ctx.getAgentPlayerUsername(); }
    @Override public Integer gameCategoryId()        { return ctx.getGameCategoryId(); }
    @Override public String gameCode()               { return ctx.getGameCode(); }
    @Override public Integer currencyId()            { return ctx.getCurrencyId(); }
    @Override public Long timestamp()                { return ctx.getVendorTimestamp(); }
    @Override public Integer status()                { return 0; }
}
