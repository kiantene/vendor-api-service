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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletBalanceServiceWrapper {
    private final GameSessionDataService gameSessionDataService;
    private final WalletService walletService;
    private final WalletExceptionTranslator walletExceptionTranslator;

    public PlayerBalanceData process(BalanceContext context) {
        GameSession gameSession = gameSessionDataService.getGameSession(context);
        return doGetBalance(context, gameSession, LogContextHolder.get());
    }

    private PlayerBalanceData doGetBalance(
            BalanceContext context,
            GameSession gameSession,
            LogContext logContext) {

        HttpRequestLog httpRequestLog = LogContextService.toHttpRequestLog(logContext);
        try {
            BigDecimal balance = walletService.getBalance(
                    httpRequestLog.getId(),
                    gameSession,
                    httpRequestLog
            );

            return PlayerBalanceData.builder()
                    .username(context.getVendorPlayerUsername())
                    .currency(context.getVendorCurrency())
                    .balance(balance)
                    .timestamp(httpRequestLog.getOperatorEnd())
                    .build();

        } catch (Exception ex) {
            walletExceptionTranslator.translateAndThrow(ex);
            return null; // Never reached, but satisfies compiler
        } finally {
            LogContextService.updateLogContextFromHttpRequestLog(logContext, httpRequestLog);
        }
    }
}
