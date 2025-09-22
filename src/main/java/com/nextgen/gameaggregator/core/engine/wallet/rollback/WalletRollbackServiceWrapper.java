package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.BetNotFoundException;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.exception.RollbackNotAllowedException;
import com.nextgen.gameaggregator.core.exception.RoundNotFoundException;
import com.nextgen.gameaggregator.core.exception.translator.WalletExceptionTranslator;
import com.nextgen.gameaggregator.core.idempotency.DuplicateRequestGuard;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.SettledBetDataService;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.TxnType;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletRollbackServiceWrapper {
    private static final String LOG_GROUP = "wallet";
    private static final String ACTION = "rollback";
    private static final long DEFAULT_DELAY_MILLISECONDS = 1000L;
    private final DuplicateRequestGuard guard;
    private final BetRollbackContextEnricher enricher;
    private final SettledBetDataService settledBetDataService;
    private final RollbackDataMapper rollbackDataMapper;
    private final BetRollbackProcessor processor;
    private final WalletService walletService;
    private final WalletExceptionTranslator walletExceptionTranslator;
    private final LogContextService logContextService;

    public PlayerBalanceData process() {
        LogContext logContext = LogContextHolder.get().setLogGroup(LOG_GROUP).setType(ACTION);
        HttpRequestLog httpRequestLog = LogContextService.toHttpRequestLog(logContext);
        BetRollbackContext context = state().getBetRollbackContext();

        try {
            GameTransaction txn = guard.ensureNotDuplicate(
                    TxnType.ROLLBACK,
                    logContext.getVendorClassName(),
                    context.getIdempotencyKey(),
                    logContext.getStart()
            );

            enricher.enrichGameTransaction(txn, context);

            BetRollbackConfig config = state().getConfig();

            if (config.isRollbackByRound()) {
                return processor.processRoundRollback(context, txn, config);
            }
            return processor.processBetRollback(context, txn, config);

        } catch (DuplicateRequestException ex) {
            return handleDuplicateRequest(context, ex);
        } catch (RoundNotFoundException ex) {
            throw InternalServerException.causedBy(ex, Map.of(
                    VendorRequestContext.KEY, context
            ));
        } catch (BetNotFoundException ex) {
            throw new RollbackNotAllowedException(context, ex);
        } catch (Exception ex) {
            guard.clear();
            throw walletExceptionTranslator.translate(ex, context);
        } finally {
            cleanup();
            LogContextService.updateLogContextFromHttpRequestLog(logContext, httpRequestLog);
        }
    }

    public void processAsync(BetRollbackContext context, long delayMilliseconds) {
        if (context == null) throw new IllegalArgumentException("BetRollbackContext cannot be null");

        LogContext logContext = LogContextHolder.get().setLogGroup(LOG_GROUP).setType(ACTION);
        boolean hasException = false;

        try {
            enricher.enrichAsync(context, logContext);
            final BetRollbackContext asyncCtx = context;
            final LogContext asyncLogCtx = logContext.copy(); // creates a copy for CompletableFuture to avoid data race

            CompletableFuture.runAsync(
                    () -> processAsyncRollbackSettledBets(asyncCtx, asyncLogCtx),
                    CompletableFuture.delayedExecutor(delayMilliseconds, TimeUnit.MILLISECONDS)
            );
        } catch (Exception ex) {
            hasException = true;
            throw walletExceptionTranslator.translate(ex, context);
        } finally {
            if (hasException) {
                LogContextService.updateLogContextFromHttpRequestLog(logContext, context.getHttpRequestLog());
            }
        }
    }

    public void processAsync(BetRollbackContext context) {
        processAsync(context, DEFAULT_DELAY_MILLISECONDS);
    }

    public WalletRollbackServiceWrapper initialise(BetRollbackContext context) {
        BetRollbackWrapperContext state = new BetRollbackWrapperContext(context);
        BetRollbackContextHolder.set(state);
        return this;
    }

    public WalletRollbackServiceWrapper configure(Consumer<BetRollbackConfig> configurer) {
        configurer.accept(state().getConfig());
        return this;
    }

    private BetRollbackWrapperContext state() {
        return BetRollbackContextHolder.getRequired();
    }

    private void cleanup() {
        guard.cleanup();
        BetRollbackContextHolder.clear();
    }

    private PlayerBalanceData handleDuplicateRequest(BetRollbackContext context, DuplicateRequestException ex) {
        GameTransaction txn = ex.getTransaction();
        if (state().getConfig().isReturnSuccessOnDuplicate() && txn.isSuccess()) {
            return PlayerBalanceData.getDefault(
                    context.getVendorPlayerUsername(),
                    context.getVendorCurrency()
            );
        }
        throw ex;
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
                processRollbackTransaction(context);
            }
        } catch (Exception ex) {
            throw walletExceptionTranslator.translate(ex, context);
        } finally {
            LogContextService.updateLogContextFromHttpRequestLog(logContext, context.getHttpRequestLog());
            logContextService.logApiRequest(logContext, "");
        }
    }

    private PlayerBalanceData processRollbackTransaction(BetRollbackContext context) throws
            InvalidAgentApiCredentialException, VendorCurrencyNotSupportException,
            BetResultIdempotentViolationException, BetRefundIdempotentViolationException, TransactionStillProcessingException,
            InvalidOperatorResponseException, InvalidFormatException,
            com.nextgen.gameaggregator.exception.BetNotFoundException,
            com.nextgen.gameaggregator.exception.RecordNotFoundException {

        return processRollbackByBet(context);
    }

    private PlayerBalanceData processRollbackByBet(BetRollbackContext context) throws
            InvalidAgentApiCredentialException, VendorCurrencyNotSupportException,
            BetResultIdempotentViolationException, BetRefundIdempotentViolationException,
            TransactionStillProcessingException, InvalidOperatorResponseException, InvalidFormatException,
            com.nextgen.gameaggregator.exception.BetNotFoundException,
            com.nextgen.gameaggregator.exception.RecordNotFoundException {

        BetRollbackConfig config = state().getConfig();
        HttpRequestLog httpRequestLog = context.getHttpRequestLog();
        GameSession gameSession = context.getGameSession();

        BigDecimal balance = walletService.processRollback(
                httpRequestLog.getId(),
                rollbackDataMapper.toRollbackData(context, config),
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
