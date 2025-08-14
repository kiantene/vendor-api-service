package com.nextgen.gameaggregator.core.engine.wallet.balance;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
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
public class WalletBalanceServiceWrapper {
    private final GameSessionDataService gameSessionDataService;
    private final WalletService walletService;
    private final WalletExceptionTranslator walletExceptionTranslator;

    public PlayerBalanceData process(@NotNull BalanceContext context) {
        LogContext logContext = LogContextHolder.get();
        logContext.setLogGroup("Balance");
        HttpRequestLog httpRequestLog = LogContextService.toHttpRequestLog(logContext);

        try {
            GameSession gameSession = gameSessionDataService.getGameSession(context);
            return doGetBalance(context, gameSession, httpRequestLog);
        } finally {
            LogContextService.updateLogContextFromHttpRequestLog(logContext, httpRequestLog);
        }
    }

    private PlayerBalanceData doGetBalance(
            BalanceContext context,
            GameSession gameSession,
            HttpRequestLog httpRequestLog) {

        try {
            BigDecimal balance = walletService.getBalance(
                    httpRequestLog.getId(),
                    gameSession,
                    httpRequestLog
            );

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
}
