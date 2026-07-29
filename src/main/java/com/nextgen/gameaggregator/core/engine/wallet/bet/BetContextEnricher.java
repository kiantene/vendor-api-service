package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.core.context.BaseEnricher;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.*;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import org.springframework.stereotype.Service;

@Service
class BetContextEnricher extends BaseEnricher<BetContext> {

    public BetContextEnricher(AgentPlayerDataService agentPlayerDataService,
                              VendorPlayerDataService vendorPlayerDataService,
                              VendorGameDataService vendorGameDataService,
                              CurrencyDataService currencyDataService,
                              VendorCurrencyDataService vendorCurrencyDataService) {
        super(agentPlayerDataService, vendorPlayerDataService, vendorGameDataService, currencyDataService, vendorCurrencyDataService);
    }

    @Override
    protected void doEnrich(BetContext context) {
        LogContext logContext = LogContextHolder.get();

        if (context.getTraceId() == null) {
            context.setTraceId(logContext.getTraceId());
        }
        if (context.getTimestamp() == null) {
            context.setTimestamp(logContext.getStart());
        }
        if (context.getVendorBetId() == null) {
            context.setVendorBetId(context.getIdempotencyKey());
        }

        // populateLogContext must be run in doEnrich so that context object will contain all required fields
        LogContextService.populateLogContext(LogContextHolder.get(), context);
    }

    public void enrichByGameSession(BetContext context, GameSession gameSession) {
        // null check is done in gameSessionDataService.getGameSession, so we won't do null check here
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

    public void enrichGameTransaction(GameTransaction txn, BetContext context) {
        txn.setVendorBetId(context.getVendorBetId());
        txn.setVendorId(context.getVendorId());
        txn.setUsername(context.getVendorPlayerUsername());
        txn.setRoundId(context.getRoundId());
        txn.setGameCode(context.getVendorGameCode());
        txn.setCurrency(context.getVendorCurrency());
        txn.setBetAmount(context.getBetAmount());
        txn.setBetTime(context.getTimestamp());
    }
}
