package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.core.context.BaseEnricher;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
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
        LogContext logContext = LogContextHolder.get();

        if (context.getTraceId() == null) {
            context.setTraceId(logContext.getTraceId());
        }

        if (context.getTimestamp() == null) {
            context.setTimestamp(logContext.getStart());
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

        if (context.getVendorCurrency() == null) {
            context.setVendorCurrency(
                    round != null
                    ? round.getCurrency() : gameSession.getVendorCurrencyCode()
            );
        }

        if (context.getGameSession() == null) {
            context.setGameSession(gameSession);
        }

        if (context.getVendorService() == null) {
            context.setVendorService(InternalVendorService.getInstance(applicationContext));
        }

        if (context.getHttpRequestLog() == null) {
            context.setHttpRequestLog(LogContextService.toHttpRequestLog(logContext));
        }

        if (context.getTimestamp() == null) {
            context.setTimestamp(logContext.getStart());
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
            context.setTimestamp(logContext.getStart());
        }
    }

    public void enrichGameTransaction(GameTransaction txn, BetRollbackContext context) {
        txn.setVendorBetId(context.getVendorBetId());
        txn.setRoundId(context.getRoundId());
    }

    public void enrichByGameRound(BetRollbackContext context, GameRound round, GameTransaction txn) {
        txn.setVendorId(round.getVendorId());
        txn.setUsername(round.getUsername());
        txn.setGameCode(round.getGameCode());
        txn.setCurrency(round.getCurrency());
        context.setVendorId(round.getVendorId());
        context.setVendorPlayerUsername(round.getUsername());
        context.setVendorGameCode(round.getGameCode());
        context.setVendorCurrency(round.getCurrency());
        enrich(context);
    }
}
