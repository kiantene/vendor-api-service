package com.nextgen.gameaggregator.service.data.producer;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import lombok.Data;

@Data
public class BetHistoryContext {
    private String externalTransactionId;
    private Integer vendorGameId;
    private Long vendorPlayerId;
    private Integer vendorId;
    private Integer vendorLineId;
    private Long agentPlayerId;
    private Integer currencyId;
    private Integer gameCategoryId;
    private Long vendorSettleTime;
    private Long resultTime;

    public static BetHistoryContext of(BetResultContext context) {
        BetHistoryContext ctx = new BetHistoryContext();

        ctx.setExternalTransactionId(context.getIdempotencyKey());
        ctx.setVendorGameId(context.getVendorGameId());
        ctx.setVendorPlayerId(context.getVendorPlayerId());
        ctx.setVendorId(context.getVendorId());
        ctx.setVendorLineId(context.getVendorLineId());
        ctx.setAgentPlayerId(context.getAgentPlayerId());
        ctx.setCurrencyId(context.getCurrencyId());
        ctx.setGameCategoryId(context.getGameCategoryId());
        ctx.setResultTime(context.getResultTime());

        return ctx;
    }

    public static BetHistoryContext of(BetRollbackContext context) {
        BetHistoryContext ctx = new BetHistoryContext();

        ctx.setExternalTransactionId(context.getIdempotencyKey());
        ctx.setVendorGameId(context.getVendorGameId());
        ctx.setVendorPlayerId(context.getVendorPlayerId());
        ctx.setVendorId(context.getVendorId());
        ctx.setVendorLineId(context.getVendorLineId());
        ctx.setAgentPlayerId(context.getAgentPlayerId());
        ctx.setCurrencyId(context.getCurrencyId());
        ctx.setGameCategoryId(context.getGameCategoryId());
        ctx.setResultTime(context.getTimestamp());

        return ctx;
    }
}
