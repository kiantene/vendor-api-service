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
import com.nextgen.gameaggregator.exception.*;
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
    private static final long DEFAULT_DELAY_MILLISECONDS = 1000L;
    private final ApplicationContext applicationContext;
    private final WalletService walletService;
    private final GameSessionDataService gameSessionDataService;
    private final SettledBetDataService settledBetDataService;
    private final RollbackDataMapper rollbackDataMapper;
    private final WalletExceptionTranslator walletExceptionTranslator;
    private final LogContextService logContextService;

    public PlayerBalanceData process(BetRollbackContext context) {
        if (context == null) throw new IllegalArgumentException("BetRollbackContext cannot be null");

        LogContext logContext = LogContextHolder.get();

        try {
            // TODO: add duplicate checks, but will return success
            enrich(context, logContext);
            return processRollbackTransaction(context, context.getGameSession(), context.getHttpRequestLog());
        } catch (Exception ex) {
            walletExceptionTranslator.translateAndThrow(ex);
        } finally {
            LogContextService.updateLogContextFromHttpRequestLog(logContext, context.getHttpRequestLog());
        }
        return null;
    }

    public void processAsync(BetRollbackContext context, long delayMilliseconds) {
        if (context == null) throw new IllegalArgumentException("BetRollbackContext cannot be null");

        LogContext logContext = LogContextHolder.get();
        boolean hasException = false;

        try {
            enrich(context, logContext);
            final BetRollbackContext asyncCtx = context;
            final LogContext asyncLogCtx = logContext.copy(); // creates a copy for CompletableFuture to avoid data race

            CompletableFuture.runAsync(
                    () -> processAsyncRollbackSettledBets(asyncCtx, asyncLogCtx),
                    CompletableFuture.delayedExecutor(delayMilliseconds, TimeUnit.MILLISECONDS)
            );
        } catch (Exception ex) {
            hasException = true;
            walletExceptionTranslator.translateAndThrow(ex);
        } finally {
            if (hasException) {
                LogContextService.updateLogContextFromHttpRequestLog(logContext, context.getHttpRequestLog());
            }
        }
    }

    public void processAsync(BetRollbackContext context) {
        processAsync(context, DEFAULT_DELAY_MILLISECONDS);
    }

    private void enrich(BetRollbackContext context, LogContext logContext) {
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
    private void processAsyncRollbackSettledBets(BetRollbackContext context, LogContext logContext) {
        try {
            if (!context.isRetrieveSettledBet() || settledBetDataService.prepareSettledBets(context.getVendorBetId(), context.getTimestamp())) {
                processRollbackTransaction(context, context.getGameSession(), context.getHttpRequestLog());
            }
        } catch (Exception ex) {
            walletExceptionTranslator.translateAndThrow(ex);
        } finally {
            LogContextService.updateLogContextFromHttpRequestLog(logContext, context.getHttpRequestLog());
            logContextService.logApiRequest(logContext, "");
        }
    }

    private PlayerBalanceData processRollbackTransaction(
            BetRollbackContext context,
            GameSession gameSession,
            HttpRequestLog httpRequestLog) throws
            InvalidAgentApiCredentialException, RecordNotFoundException, VendorCurrencyNotSupportException,
            BetResultIdempotentViolationException, BetRefundIdempotentViolationException, TransactionStillProcessingException,
            InvalidOperatorResponseException, BetNotFoundException, InvalidFormatException {

        BigDecimal balance = walletService.processRollback(
                httpRequestLog.getId(),
                rollbackDataMapper.toRollbackData(context),
                gameSession,
                context.getVendorService(),
                httpRequestLog
        );

        return new PlayerBalanceData(
                context.getVendorPlayerUsername(),
                gameSession.getVendorCurrencyCode(),
                balance,
                httpRequestLog.getOperatorEnd()
        );
    }
}
