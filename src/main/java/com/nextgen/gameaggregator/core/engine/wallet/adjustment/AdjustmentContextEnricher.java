package com.nextgen.gameaggregator.core.engine.wallet.adjustment;

import com.nextgen.gameaggregator.core.context.BaseEnricher;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.*;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.enums.GameRoundState;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
class AdjustmentContextEnricher extends BaseEnricher<AdjustmentContext> {

    public AdjustmentContextEnricher(AgentPlayerDataService agentPlayerDataService,
                                     VendorPlayerDataService vendorPlayerDataService,
                                     VendorGameDataService vendorGameDataService,
                                     CurrencyDataService currencyDataService,
                                     VendorCurrencyDataService vendorCurrencyDataService) {
        super(agentPlayerDataService, vendorPlayerDataService, vendorGameDataService, currencyDataService, vendorCurrencyDataService);
    }

    @Override
    protected void doEnrich(AdjustmentContext context) {
        setDefaultIfEmpty(context);

        // populateLogContext must be run in doEnrich so that context object will contain all required fields
        LogContext logContext = LogContextHolder.get();
        LogContextService.populateLogContext(logContext, context);

        context.setTraceId(logContext.getTraceId());

        if (context.getTimestamp() == null) {
            context.setTimestamp(logContext.getStart());
        }
    }

    public void enrichByGameSession(AdjustmentContext context, GameSession gameSession) {
        if (context.getVendorPlayerUsername() == null) {
            context.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
        }
        if (context.getVendorCurrency() == null) {
            context.setVendorCurrency(gameSession.getVendorCurrencyCode());
        }
        if (context.getVendorGameCode() == null) {
            context.setVendorGameCode(gameSession.getVendorGameCode());
        }
        if (context.getCurrencyCode() == null) {
            context.setCurrencyCode(gameSession.getCurrencyCode());
        }
        
        enrich(context);
    }

    public void enrichGameTransaction(GameTransaction txn, AdjustmentContext context) {
        txn.setVendorBetId(context.getVendorBetId());
        txn.setVendorId(context.getVendorId());
        txn.setUsername(context.getVendorPlayerUsername());
        txn.setRoundId(context.getRoundId());
        txn.setGameCode(context.getVendorGameCode());
        txn.setCurrency(context.getVendorCurrency());
        txn.setWinAmount(context.getWinAmount());
        txn.setSettleTime(context.getTimestamp());
        txn.setState(GameRoundState.SETTLED);
    }

    public Mono<Void> enrichGameTransactionIfEmpty(GameTransaction txn, AdjustmentContext context) {
        return Mono.defer(() -> {
            // if idx is null, means that an exception is thrown before enrichGameTransaction is called
            if (txn.getIdx() == null) {
                return Mono.fromRunnable(() -> enrichGameTransaction(txn, context));
            }
            return Mono.empty();
        });
    }

    private void setDefaultIfEmpty(AdjustmentContext context) {

        if (context.getVendorBetId() == null) {
            context.setVendorBetId(context.getIdempotencyKey());
        }
    }
}
