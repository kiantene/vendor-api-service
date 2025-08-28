package com.nextgen.gameaggregator.core.engine.game.authenticate;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.balance.WalletBalanceServiceWrapper;
import com.nextgen.gameaggregator.core.exception.translator.WalletExceptionTranslator;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticateServiceWrapper implements AuthenticateService {
    private static final String LOG_GROUP = "game";
    private static final String ACTION = "auth";
    private final GameSessionDataService gameSessionDataService;
    private final WalletBalanceServiceWrapper walletService;
    private final WalletExceptionTranslator walletExceptionTranslator;

    public PlayerBalanceData process(AuthenticateContext context) {
        LogContext logContext = LogContextHolder.get().setLogGroup(LOG_GROUP).setType(ACTION);
        HttpRequestLog httpRequestLog = LogContextService.toHttpRequestLog(logContext);

        try {
            // TODO: add validator
            GameSession gameSession = gameSessionDataService.getGameSession(context);

            enrichByGameSession(context, gameSession);

            // TODO: do we need to validate gameSession status? eg. session terminated
//            if (shouldUpdateVendorToken(context, gameSession)) {
//                gameSessionDataService.updateVendorToken(gameSession, context.getVendorSessionToken());
//            }
            return walletService.getBalance(
                    context.getVendorPlayerUsername(),
                    context.getVendorCurrency(),
                    gameSession,
                    httpRequestLog
            );
        } catch (Exception ex) {
            walletExceptionTranslator.translateAndThrow(ex);
            return null; // Never reached, but satisfies compiler
        } finally {
            LogContextService.updateLogContextFromHttpRequestLog(logContext, httpRequestLog);
        }
    }

    private void enrichByGameSession(AuthenticateContext context, GameSession gameSession) {
        // null check is done in gameSessionDataService.getGameSession, so we won't do null check here
        if (context.getVendorPlayerUsername() == null) {
            context.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
        }
        if (context.getVendorCurrency() == null) {
            context.setVendorCurrency(gameSession.getVendorCurrencyCode());
        }
    }

    // TODO: to revisit this logic
//    private boolean shouldUpdateVendorToken(GameSessionData gameSessionData, GameSession gameSession) {
//        String vendorSessionToken = gameSessionData.getVendorSessionToken();
//        return vendorSessionToken != null
//                && gameSessionData.getVendorPlayerUsername() != null
//                && gameSessionData.getToken() == null // Indicates vendor-based lookup
//                && !vendorSessionToken.equals(gameSession.getVendorToken()); // update only if not the same
//    }
}
