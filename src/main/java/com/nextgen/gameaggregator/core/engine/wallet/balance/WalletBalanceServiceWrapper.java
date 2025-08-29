package com.nextgen.gameaggregator.core.engine.wallet.balance;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.translator.WalletExceptionTranslator;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.exception.VendorCurrencyNotSupportException;
import com.nextgen.gameaggregator.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletBalanceServiceWrapper implements WalletBalanceService {
    private static final String LOG_GROUP = "wallet";
    private static final String ACTION = "balance";
    private final GameSessionDataService gameSessionDataService;
    private final WalletService walletService;
    private final WalletExceptionTranslator walletExceptionTranslator;

    public PlayerBalanceData process(BalanceContext context) {
        LogContext logContext = LogContextHolder.get().setLogGroup(LOG_GROUP).setType(ACTION);
        HttpRequestLog httpRequestLog = LogContextService.toHttpRequestLog(logContext);

        try {
            GameSession gameSession = gameSessionDataService.getGameSession(context);

            enrichByGameSession(context, gameSession);

            // TODO: add validator
            return getBalance(
                    context.getVendorPlayerUsername(),
                    context.getVendorCurrency(),
                    gameSession,
                    httpRequestLog
            );
        } catch (Exception ex) {
            walletExceptionTranslator.translateAndThrow(ex);
        } finally {
            LogContextService.updateLogContextFromHttpRequestLog(logContext, httpRequestLog);
        }
        return null;
    }

    private void enrichByGameSession(BalanceContext context, GameSession gameSession) {
        // null check is done in gameSessionDataService.getGameSession, so we won't do null check here
        if (context.getVendorPlayerUsername() == null) {
            context.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
        }
        if (context.getVendorCurrency() == null) {
            context.setVendorCurrency(gameSession.getVendorCurrencyCode());
        }
    }

    public PlayerBalanceData getBalance(
            String playerUsername,
            String currency,
            GameSession gameSession,
            HttpRequestLog httpRequestLog) throws InvalidAgentApiCredentialException, VendorCurrencyNotSupportException, InvalidOperatorResponseException {

        // TODO: new logic -> if operator returns SC_USER_DISABLED, need to map and throw PlayerDisabledException
        BigDecimal balance = walletService.getBalance(
                httpRequestLog.getId(),
                gameSession,
                httpRequestLog
        );

        return new PlayerBalanceData(
                playerUsername,
                currency,
                balance,
                httpRequestLog.getOperatorEnd()
        );
    }
}
