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

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class AuthenticateServiceWrapper implements AuthenticateService {
    private static final String LOG_GROUP = "game";
    private static final String ACTION = "auth";
    private final GameSessionDataService gameSessionDataService;
    private final WalletBalanceServiceWrapper walletService;
    private final WalletExceptionTranslator walletExceptionTranslator;

    public PlayerBalanceData process() {
        AuthenticateContext context = state().getAuthContext();
        LogContext logContext = LogContextHolder.get().setLogGroup(LOG_GROUP).setType(ACTION);
        HttpRequestLog httpRequestLog = LogContextService.toHttpRequestLog(logContext);
        AuthConfig config = state().getConfig();

        try {
            // TODO: add validator
            GameSession gameSession = gameSessionDataService.getGameSession(context);

            enrichByGameSession(context, gameSession);

            if (config.shouldRefreshToken()) {
                // TODO: refresh token
            }
            // TODO: do we need to validate gameSession status? eg. session terminated

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

    @Override
    public AuthenticateService initialise(AuthenticateContext context) {
        AuthWrapperContext state = new AuthWrapperContext(context);
        AuthContextHolder.set(state);
        return this;
    }

    private AuthWrapperContext state() {
        return AuthContextHolder.getRequired();
    }

    @Override
    public AuthenticateService configure(Consumer<AuthConfig> configurer) {
        configurer.accept(state().getConfig());
        return this;
    }
}
