package com.nextgen.gameaggregator.core.engine.game.authenticate;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.game.GameSessionData;
import com.nextgen.gameaggregator.core.exception.translator.WalletExceptionTranslator;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.WalletService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AuthenticateServiceWrapper {
    private final GameSessionDataService gameSessionDataService;
    private final WalletService walletService;
    private final WalletExceptionTranslator walletExceptionTranslator;

    public PlayerBalanceData process(@NotNull AuthenticateContext context) {
        LogContext logContext = LogContextHolder.get();
        logContext.setLogGroup("Authenticate");
        HttpRequestLog httpRequestLog = LogContextService.toHttpRequestLog(logContext);

        try {
            GameSession gameSession = gameSessionDataService.getGameSession(context);
            if (shouldUpdateVendorToken(context, gameSession)) {
                gameSessionDataService.updateVendorToken(gameSession, context.getVendorSessionToken());
            }
            return getBalance(context, gameSession, httpRequestLog);
        } finally {
            LogContextService.updateLogContextFromHttpRequestLog(logContext, httpRequestLog);
        }
    }

    private PlayerBalanceData getBalance(AuthenticateContext context, GameSession gameSession, HttpRequestLog httpRequestLog) {
        try {
            BigDecimal balance = walletService.getBalance(httpRequestLog.getId(), gameSession, httpRequestLog);

            return new PlayerBalanceData(
                    context.getVendorPlayerUsername(),
                    context.getVendorCurrency(),
                    balance,
                    httpRequestLog.getOperatorEnd()
            );

        } catch (Exception ex) {
            walletExceptionTranslator.translateAndThrow(ex);
            return null; // Never reached, but satisfies compiler
        }
    }

    private boolean shouldUpdateVendorToken(GameSessionData gameSessionData, GameSession gameSession) {
        String vendorSessionToken = gameSessionData.getVendorSessionToken();
        return vendorSessionToken != null
                && gameSessionData.getVendorPlayerUsername() != null
                && gameSessionData.getToken() == null // Indicates vendor-based lookup
                && !vendorSessionToken.equals(gameSession.getVendorToken()); // update only if not the same
    }
}
