package com.nextgen.gameaggregator.core.engine.wallet.credit;

import com.nextgen.gameaggregator.core.context.BaseEnricher;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.AgentPlayerDataService;
import com.nextgen.gameaggregator.core.service.CurrencyDataService;
import com.nextgen.gameaggregator.core.service.VendorCurrencyDataService;
import com.nextgen.gameaggregator.core.service.VendorGameDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.entity.couchbase.AgentMeta;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Populates a {@link CreditContext}'s agent / vendor / currency / game fields from the
 * resolved {@link GameSession} and the inherited {@link BaseEnricher} chain. Mirrors
 * {@link com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextEnricher BetContextEnricher}.
 * Invoked from {@link WalletCreditServiceWrapper#process()} after the framework-level
 * session lookup.
 */
@Slf4j
@Service
class CreditContextEnricher extends BaseEnricher<CreditContext> {

    public CreditContextEnricher(AgentPlayerDataService agentPlayerDataService,
                                 VendorPlayerDataService vendorPlayerDataService,
                                 VendorGameDataService vendorGameDataService,
                                 CurrencyDataService currencyDataService,
                                 VendorCurrencyDataService vendorCurrencyDataService) {
        super(agentPlayerDataService, vendorPlayerDataService, vendorGameDataService,
                currencyDataService, vendorCurrencyDataService);
    }

    @Override
    protected void doEnrich(CreditContext context) {
        // Mirrors BetContextEnricher.doEnrich: backfill traceId from the request-scoped
        // LogContext so ClientResponseValidator and downstream logging have one.
        LogContext logContext = LogContextHolder.get();
        if (context.getTraceId() == null) {
            context.setTraceId(logContext.getTraceId());
        }
        LogContextService.populateLogContext(LogContextHolder.get(), context);
    }

    public void enrichByGameSession(CreditContext context, GameSession gameSession) {
        // Source the operator token from the resolved session (mirrors how bet/result read
        // gameSession.getToken()). On an aged-out refund the request token is null, so this
        // picks up the token minted during session regeneration; a no-op when both are null.
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

    /**
     * For a credit resolved against its debit ({@link CreditSessionResolver}): after
     * {@link #enrichByGameSession}, pin who the operator call goes to on the debit's snapshot, not
     * on today's lookup of the player. The credit must return to the agent and agent player the
     * debit took the money from; a lookup that has since changed, or a stale cache, must not
     * redirect it. Currency and rates are left to the lookup: a vendor player's currency is fixed,
     * so the lookup gives the debit's operator currency, and rates are vendor-currency config, read
     * per request.
     */
    public void applyDebitSnapshot(CreditContext context, AgentMeta snapshot) {
        if (!Objects.equals(snapshot.getAgentId(), context.getAgentId())
                || !Objects.equals(snapshot.getUsername(), context.getAgentPlayerUsername())) {
            log.warn("Credit {}::{}: player lookup gives agent {} / {}, the referenced debit was made "
                            + "for agent {} / {}; crediting the debit's",
                    context.getVendorClassName(), context.getVendorTxnId(),
                    context.getAgentId(), context.getAgentPlayerUsername(),
                    snapshot.getAgentId(), snapshot.getUsername());
        }
        context.setAgentId(snapshot.getAgentId());
        context.setAgentPlayerUsername(snapshot.getUsername());
        if (snapshot.getGameCode() != null) {
            context.setGameCode(snapshot.getGameCode());
        }
    }
}
