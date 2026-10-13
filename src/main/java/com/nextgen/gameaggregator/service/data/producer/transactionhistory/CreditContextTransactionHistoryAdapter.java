package com.nextgen.gameaggregator.service.data.producer.transactionhistory;

import com.nextgen.gameaggregator.core.engine.wallet.credit.CreditContext;

/**
 * Adapts a {@link CreditContext} (settle or refund) to {@link TransactionHistoryContext}.
 * The discriminator-on-disk between settle vs refund lives on the underlying
 * {@code CreditTxn.state} (SETTLED / REFUNDED), not on the kafka emission's
 * {@code transactionType} field — both modes emit as
 * {@link com.nextgen.gameaggregator.enums.BetTransactionType#CREDIT}.
 */
public final class CreditContextTransactionHistoryAdapter implements TransactionHistoryContext {

    private final CreditContext ctx;

    public CreditContextTransactionHistoryAdapter(CreditContext ctx) {
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
