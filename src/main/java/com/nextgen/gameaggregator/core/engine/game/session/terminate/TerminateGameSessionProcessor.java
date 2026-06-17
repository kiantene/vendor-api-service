package com.nextgen.gameaggregator.core.engine.game.session.terminate;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class TerminateGameSessionProcessor {
    private final GameSessionDataService gameSessionDataService;
    private static final String LOG_GROUP = "game";
    private static final String ACTION = "terminate";

    public void process(TerminateGameSessionContext context)
            throws GameSessionExpiredException, InternalServerException {

        LogContext logContext = LogContextHolder.get().setLogGroup(LOG_GROUP).setType(ACTION);
        HttpRequestLog httpRequestLog = LogContextService.toHttpRequestLog(logContext);

        //Get game session
        try {
            GameSession gameSession = gameSessionDataService.getGameSession(context);

            // Terminate the game session
            gameSessionDataService.terminateGameSession(gameSession);
        } finally {
            LogContextService.updateLogContextFromHttpRequestLog(logContext, httpRequestLog);
        }
    }
}
