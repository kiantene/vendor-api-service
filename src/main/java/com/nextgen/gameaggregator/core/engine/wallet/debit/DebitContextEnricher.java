package com.nextgen.gameaggregator.core.engine.wallet.debit;

import com.nextgen.gameaggregator.core.context.BaseEnricher;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.AgentPlayerDataService;
import com.nextgen.gameaggregator.core.service.CurrencyDataService;
import com.nextgen.gameaggregator.core.service.VendorCurrencyDataService;
import com.nextgen.gameaggregator.core.service.VendorGameDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import org.springframework.stereotype.Service;

/**
 * Populates a {@link DebitContext}'s agent / vendor / currency / game fields from the
 * resolved {@link GameSession} and the inherited {@link BaseEnricher} chain
 * (vendorPlayer → agentPlayer → vendorGame → vendorCurrency lookups).
 * <p>
 * Mirrors {@link com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextEnricher
 * BetContextEnricher} for the BET path. Invoked from
 * {@link WalletDebitServiceWrapper#process()} after the framework-level session lookup.
 */
@Service
class DebitContextEnricher extends BaseEnricher<DebitContext> {

    public DebitContextEnricher(AgentPlayerDataService agentPlayerDataService,
                                VendorPlayerDataService vendorPlayerDataService,
                                VendorGameDataService vendorGameDataService,
                                CurrencyDataService currencyDataService,
                                VendorCurrencyDataService vendorCurrencyDataService) {
        super(agentPlayerDataService, vendorPlayerDataService, vendorGameDataService,
                currencyDataService, vendorCurrencyDataService);
    }

    @Override
    protected void doEnrich(DebitContext context) {
        // Mirrors BetContextEnricher.doEnrich: backfill traceId from the request-scoped
        // LogContext so ClientResponseValidator and downstream logging have one.
        LogContext logContext = LogContextHolder.get();
        if (context.getTraceId() == null) {
            context.setTraceId(logContext.getTraceId());
        }
        // populateLogContext runs here so the MDC mirrors the (now-enriched) context fields.
        LogContextService.populateLogContext(LogContextHolder.get(), context);
    }

    /**
     * Backfills context fields from the resolved session before running the
     * {@link BaseEnricher} chain. Mirrors {@code BetContextEnricher.enrichByGameSession}.
     */
    public void enrichByGameSession(DebitContext context, GameSession gameSession) {
        // Source the operator token from the resolved session, mirroring CreditContextEnricher
        // (and how bet/result read gameSession.getToken()). DebitOperatorInvoker puts this on the
        // OperatorCallTarget, so without it a vendor whose wire payload carries no GA token would
        // call the operator with a null session. A no-op when the request already carried one.
        if (context.getToken() == null) {
            context.setToken(gameSession.getToken());
        }
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
}
