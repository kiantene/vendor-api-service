package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.translator.WalletExceptionTranslator;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.core.service.InternalVendorService;
import com.nextgen.gameaggregator.core.service.SettledBetDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class WalletRollbackServiceWrapper {
    private static final ThreadLocal<BetRollbackWrapperContext> stateHolder = new ThreadLocal<>();
    private static final long DEFAULT_DELAY_MILLISECONDS = 1000L;
    private final ApplicationContext applicationContext;
    private final WalletService walletService;
    private final GameSessionDataService gameSessionDataService;
    private final SettledBetDataService settledBetDataService;
    private final RollbackDataMapper rollbackDataMapper;
    private final WalletExceptionTranslator walletExceptionTranslator;
    private final LogContextService logContextService;

    public WalletRollbackServiceWrapper initialise(BetRollbackContext context) {
        BetRollbackWrapperContext state = new BetRollbackWrapperContext(context);
        stateHolder.set(state);
        return this;
    }

    public PlayerBalanceData process(BetRollbackContext context) {
        enrich(context);
        return processRollbackTransaction(context, context.getGameSession(), LogContextHolder.get(), false);
    }

    public void processAsync(BetRollbackContext context) {
        processAsync(context, DEFAULT_DELAY_MILLISECONDS);
    }

    public void processAsync(BetRollbackContext context, long delayMilliseconds) {
        LogContext logContext = LogContextHolder.get();
        enrich(context);
        CompletableFuture.runAsync(
                () -> processRollbackSettledBets(context, logContext),
                CompletableFuture.delayedExecutor(delayMilliseconds, TimeUnit.MILLISECONDS)
        );
    }

    private void enrich(BetRollbackContext context) {
        if (context == null) {
            throw new IllegalArgumentException("BetRollbackContext cannot be null");
        }

        LogContext logContext = LogContextHolder.get();
        logContext.setLogGroup("Rollback");

        if (context.getGameSession() == null) {
            context.setGameSession(gameSessionDataService.getOrCreate(context));
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

    /**
     * If settled bet retrieval is required, prepareSettledBets must be successful
     * in order to proceed with the rollback.
     * OR
     * If settled bet retrieval is NOT required, just proceed with rollback.
     */
    private void processRollbackSettledBets(BetRollbackContext context, LogContext logContext) {
        if (!context.isRetrieveSettledBet() || settledBetDataService.prepareSettledBets(context.getVendorBetId(), context.getTimestamp())) {
            processRollbackTransaction(context, context.getGameSession(), logContext, true);
        }
    }

    private PlayerBalanceData processRollbackTransaction(
            BetRollbackContext context,
            GameSession gameSession,
            LogContext logContext,
            boolean isAsync) {

        HttpRequestLog httpRequestLog = context.getHttpRequestLog();
        try {
            BigDecimal balance = walletService.processRollback(
                    httpRequestLog.getId(),
                    rollbackDataMapper.toRollbackData(context),
                    gameSession,
                    context.getVendorService(),
                    httpRequestLog
            );

            return PlayerBalanceData.builder()
                    .username(context.getVendorPlayerUsername())
                    .currency(gameSession.getVendorCurrencyCode())
                    .balance(balance)
                    .timestamp(httpRequestLog.getOperatorEnd())
                    .build();

        } catch (Exception ex) {
            walletExceptionTranslator.translateAndThrow(ex);
            return null;
        } finally {
            cleanup();
            LogContextService.updateLogContextFromHttpRequestLog(logContext, httpRequestLog);
            if (isAsync) {
                logContextService.logApiRequest(logContext, "");
            }
        }
    }

    private void cleanup() {
        stateHolder.remove();
    }
}
