package com.nextgen.gameaggregator.core.engine.game.authenticate;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.game.GameSessionData;
import com.nextgen.gameaggregator.core.engine.wallet.balance.WalletBalanceServiceWrapper;
import com.nextgen.gameaggregator.core.exception.translator.WalletExceptionTranslator;
import com.nextgen.gameaggregator.core.logging.*;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticateServiceWrapper {
    private final GameSessionDataService gameSessionDataService;
    private final WalletBalanceServiceWrapper walletService;
    private final WalletExceptionTranslator walletExceptionTranslator;

    public PlayerBalanceData process(@NotNull AuthenticateContext context) {
        LogContext logContext = LogContextHolder.get();
        logContext.setLogGroup("Authenticate");
        HttpRequestLog httpRequestLog = LogContextService.toHttpRequestLog(logContext);

        try {
            // TODO: add validator
            GameSession gameSession = gameSessionDataService.getGameSession(context);
            // TODO: do we need to validate gameSession status? eg. session terminated
            if (shouldUpdateVendorToken(context, gameSession)) {
                gameSessionDataService.updateVendorToken(gameSession, context.getVendorSessionToken());
            }
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

    private boolean shouldUpdateVendorToken(GameSessionData gameSessionData, GameSession gameSession) {
        String vendorSessionToken = gameSessionData.getVendorSessionToken();
        return vendorSessionToken != null
                && gameSessionData.getVendorPlayerUsername() != null
                && gameSessionData.getToken() == null // Indicates vendor-based lookup
                && !vendorSessionToken.equals(gameSession.getVendorToken()); // update only if not the same
    }
}
