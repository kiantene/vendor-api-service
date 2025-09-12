package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.exception.translator.WalletExceptionTranslator;
import com.nextgen.gameaggregator.core.idempotency.DuplicateRequestGuard;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.core.service.SettledBetDataService;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.TxnType;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.service.business.GameRoundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class WalletRollbackServiceWrapper {
    private static final String LOG_GROUP = "wallet";
    private static final String ACTION = "rollback";
    private static final long DEFAULT_DELAY_MILLISECONDS = 1000L;
    private final DuplicateRequestGuard guard;
    private final BetRollbackContextEnricher enricher;
    private final GameSessionDataService gameSessionDataService;
    private final GameRoundService gameRoundService;
    private final SettledBetDataService settledBetDataService;
    private final RollbackDataMapper rollbackDataMapper;
    private final BetRollbackProcessor processor;
    private final WalletService walletService;
    private final WalletExceptionTranslator walletExceptionTranslator;
    private final LogContextService logContextService;

    public PlayerBalanceData process() {
        BetRollbackContext context = state().getBetRollbackContext();
        LogContext logContext = LogContextHolder.get().setLogGroup(LOG_GROUP).setType(ACTION);

        try {
            GameTransaction txn = guard.ensureNotDuplicate(
                    TxnType.ROLLBACK,
                    logContext.getVendorClassName(),
                    context.getIdempotencyKey(),
                    logContext.getStart()
            );

            GameRound round = getGameRoundInfo(logContext.getVendorClassName(), context);

            GameSession gameSession = gameSessionDataService.getOrCreate(context);

            enricher.enrichByGameSession(context, round, gameSession, logContext);

            return processRollbackTransaction(context, txn);
        } catch (DuplicateRequestException ex) {
            return handleDuplicateRequest(context, ex);
        } catch (Exception ex) {
            guard.clear();
            throw walletExceptionTranslator.translate(ex);
        } finally {
            cleanup();
            LogContextService.updateLogContextFromHttpRequestLog(logContext, context.getHttpRequestLog());
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
            throw walletExceptionTranslator.translate(ex);
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
        // TODO: check for operator status, if is successful then return success

        String currency = "";
        return PlayerBalanceData.getDefault(context.getVendorPlayerUsername(), currency);
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
                processRollbackTransaction(context, null);
            }
        } catch (Exception ex) {
            throw walletExceptionTranslator.translate(ex);
        } finally {
            LogContextService.updateLogContextFromHttpRequestLog(logContext, context.getHttpRequestLog());
            logContextService.logApiRequest(logContext, "");
        }
    }

    private PlayerBalanceData processRollbackTransaction(
            BetRollbackContext context,
            GameTransaction txn) throws
            InvalidAgentApiCredentialException, RecordNotFoundException, VendorCurrencyNotSupportException,
            BetResultIdempotentViolationException, BetRefundIdempotentViolationException, TransactionStillProcessingException,
            InvalidOperatorResponseException, BetNotFoundException, InvalidFormatException {

        BetRollbackConfig config = state().getConfig();

        if (RollbackType.BY_ROUND == config.getRollbackType()) {
            enricher.enrichGameTransaction(txn, context);
            return processRollbackByRound(context, txn);
        }

        return processRollbackByBet(context);
    }

    private PlayerBalanceData processRollbackByRound(BetRollbackContext context, GameTransaction txn) {
        return processor.process(context, txn, state().getConfig());
    }

    private PlayerBalanceData processRollbackByBet(BetRollbackContext context) throws
            InvalidAgentApiCredentialException, RecordNotFoundException, VendorCurrencyNotSupportException,
            BetResultIdempotentViolationException, BetRefundIdempotentViolationException,
            TransactionStillProcessingException, InvalidOperatorResponseException, BetNotFoundException,
            InvalidFormatException {

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

    private GameRound getGameRoundInfo(String vendorClassName, BetRollbackContext context) {
        if (state().getConfig().getRollbackType() == RollbackType.BY_ROUND) {
            GameRound search = GameRound.of(vendorClassName, context.getRoundId());
            var roundOpt = gameRoundService.get(search.getId());

            if (roundOpt.isEmpty()) {
                // TODO: to handle not found
                return null;
            }
            return roundOpt.get();
        }

        // TODO: use txn to search round
        return null;
    }
}
