package com.nextgen.gameaggregator.service.data.producer.transactionhistory;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;

public final class BetContextTransactionHistoryAdapter implements TransactionHistoryContext {

    private final BetContext ctx;
    private final Exception ex;

    public BetContextTransactionHistoryAdapter(BetContext ctx, Exception ex) {
        this.ctx = ctx;
        this.ex = ex;
    }

    @Override public String externalTransactionId() {
        return ctx.getIdempotencyKey();
    }

    @Override public String vendorBetId() {
        return ctx.getVendorBetId();
    }

    @Override public String roundId() {
        return ctx.getRoundId();
    }

    @Override public Integer vendorGameId() {
        return ctx.getVendorGameId();
    }

    @Override public Long vendorPlayerId() {
        return ctx.getVendorPlayerId();
    }

    @Override public String vendorPlayerUsername() {
        return ctx.getVendorPlayerUsername();
    }

    @Override public Integer vendorId() {
        return ctx.getVendorId();
    }

    @Override public Integer vendorLineId() {
        return ctx.getVendorLineId();
    }

    @Override public Integer agentId() {
        return ctx.getAgentId();
    }

    @Override public Long agentPlayerId() {
        return ctx.getAgentPlayerId();
    }

    @Override public String agentPlayerUsername() {
        return ctx.getAgentPlayerUsername();
    }

    @Override public Integer gameCategoryId() {
        return ctx.getGameCategoryId();
    }

    @Override public String gameCode() {
        return ctx.getGameCode();
    }

    @Override public Integer currencyId() {
        return ctx.getCurrencyId();
    }

    @Override public Long timestamp() {
        return ctx.getTimestamp();
    }

    @Override public Integer status() {
        return ex == null ? 0 : 1;
    }
}
