package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.core.context.BaseEnricher;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.*;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
class BetRollbackContextEnricher extends BaseEnricher<BetRollbackContext> {
    private final ApplicationContext applicationContext;
    private final GameSessionDataService gameSessionDataService;

    public BetRollbackContextEnricher(AgentPlayerDataService agentPlayerDataService,
                                      VendorPlayerDataService vendorPlayerDataService,
                                      VendorGameDataService vendorGameDataService,
                                      ApplicationContext applicationContext,
                                      GameSessionDataService gameSessionDataService) {
        super(agentPlayerDataService, vendorPlayerDataService, vendorGameDataService);
        this.applicationContext = applicationContext;
        this.gameSessionDataService = gameSessionDataService;
    }

    @Override
    protected void doEnrich(BetRollbackContext context) {
        if (context.getVendorService() == null) {
            context.setVendorService(InternalVendorService.getInstance(applicationContext));
        }
        if (context.getTimestamp() == null) {
            context.setTimestamp(System.currentTimeMillis());
        }
    }

    public void enrichByGameSession(BetRollbackContext context,
                                    GameRound round,
                                    GameSession gameSession,
                                    LogContext logContext) {

        context.setTraceId(logContext.getTraceId());

        if (context.getVendorPlayerUsername() == null) {
            context.setVendorPlayerUsername(
                    round != null
                    ? round.getUsername() : gameSession.getVendorPlayerUsername()
            );
        }

        if (context.getVendorGameCode() == null) {
            context.setVendorGameCode(
                    round != null
                    ? round.getGameCode() : gameSession.getVendorGameCode()
            );
        }

        if (context.getVendorCurrencyCode() == null) {
            context.setVendorCurrencyCode(
                    round != null
                    ? round.getCurrency() : gameSession.getVendorCurrencyCode()
            );
        }

        enrich(context);
    }

    public void enrichAsync(BetRollbackContext context, LogContext logContext) {
        context.setTraceId(logContext.getTraceId());

        if (context.getGameSession() == null) {
            GameSession gameSession = gameSessionDataService.getOrCreate(context);
            context.setGameSession(gameSession);
            if (context.getVendorPlayerUsername() == null) {
                context.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
            }
        }

        if (context.getVendorService() == null) {
            context.setVendorService(InternalVendorService.getInstance(applicationContext));
        }

        if (context.getHttpRequestLog() == null) {
            context.setHttpRequestLog(LogContextService.toHttpRequestLog(logContext));
        }

        if (context.getTimestamp() == null) {
            context.setTimestamp(System.currentTimeMillis());
        }
    }

    public void enrichGameTransaction(GameTransaction txn, BetRollbackContext context) {
        txn.setVendorId(context.getVendorId());
        txn.setRoundId(context.getRoundId());
    }
}
