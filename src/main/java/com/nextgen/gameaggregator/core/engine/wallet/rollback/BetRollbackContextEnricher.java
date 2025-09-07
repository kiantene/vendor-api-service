package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.core.service.InternalVendorService;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class BetRollbackContextEnricher {
    private final ApplicationContext applicationContext;
    private final GameSessionDataService gameSessionDataService;

    public BetRollbackContextEnricher(ApplicationContext applicationContext,
                                      GameSessionDataService gameSessionDataService) {
        this.applicationContext = applicationContext;
        this.gameSessionDataService = gameSessionDataService;
    }

    public void enrich(BetRollbackContext context, LogContext logContext) {
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
        txn.setRoundId(context.getRoundId());
    }
}
