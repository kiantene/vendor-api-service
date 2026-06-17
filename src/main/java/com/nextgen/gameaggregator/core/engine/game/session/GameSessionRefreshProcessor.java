package com.nextgen.gameaggregator.core.engine.game.session;

import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.service.GameSessionService;

@Component
public class GameSessionRefreshProcessor {

    private final GameSessionService gameSessionService;
    private final GameSessionDataService gameSessionDataService;
    private static final String LOG_GROUP = "game";
    private static final String ACTION = "regenerate";

    public GameSessionRefreshProcessor(GameSessionDataService gameSessionDataService,
                                       GameSessionService gameSessionService) {
        this.gameSessionDataService = gameSessionDataService;
        this.gameSessionService = gameSessionService;
    }

    /**
     * Orchestrates the replacement of an old session with a fresh token.
     */
    public GameSession process(GameSessionRefreshContext context)
            throws GameSessionExpiredException, InternalServerException, GameNotSupportedException {

        LogContext logContext = LogContextHolder.get().setLogGroup(LOG_GROUP).setType(ACTION);
        HttpRequestLog httpRequestLog = LogContextService.toHttpRequestLog(logContext);
        try {
            // 1. Fetch the existing session based on context
            GameSession gameSession = gameSessionDataService.getGameSession(context);

            // 2. Apply business rule updates (Vendor-specific changes)
            syncVendorGameMetadata(gameSession, context.getVendorGameCode());

            // 3. Create a new session
            return gameSessionDataService.createNewGameSession(gameSession);

        } finally {
            LogContextService.updateLogContextFromHttpRequestLog(logContext, httpRequestLog);
        }
    }

    private void syncVendorGameMetadata(GameSession session, String newGameCode) throws GameNotSupportedException {
        if (StringUtils.hasText(newGameCode) && !newGameCode.equals(session.getVendorGameCode())) {
            gameSessionService.updateByVendorGameCode(session, newGameCode);
        }
    }
}
