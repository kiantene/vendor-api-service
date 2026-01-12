package com.nextgen.gameaggregator.service.data.producer.transactionhistory;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;

public final class BetRollbackContextTransactionHistoryAdapter implements TransactionHistoryContext {

    private final BetRollbackContext ctx;

    public BetRollbackContextTransactionHistoryAdapter(BetRollbackContext ctx) {
        this.ctx = ctx;
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
}
