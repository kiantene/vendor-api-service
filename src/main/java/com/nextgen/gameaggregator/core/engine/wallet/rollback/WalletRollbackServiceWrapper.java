package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.WalletExceptionTranslator;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.core.service.SettledBetDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

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

    public WalletRollbackServiceWrapper initialise(BetRollbackContext context) {
        BetRollbackWrapperContext state = new BetRollbackWrapperContext(context);
        stateHolder.set(state);
        return this;
    }

    public void process(BetRollbackContext context) {

    }

    public void processAsync(BetRollbackContext context) {
        processAsync(context, DEFAULT_DELAY_MILLISECONDS);
    }

    public void processAsync(BetRollbackContext context, long delayMilliseconds) {
        enrich(context);
        CompletableFuture.runAsync(
                () -> processRollbackSettledBets(context),
                CompletableFuture.delayedExecutor(delayMilliseconds, TimeUnit.MILLISECONDS)
        );
    }

    private void enrich(BetRollbackContext context) {
        if (context == null) {
            throw new IllegalArgumentException("BetRollbackContext cannot be null");
        }

        if (context.getGameSession() == null) {
            context.setGameSession(gameSessionDataService.getOrCreate(context));
        }

        if (context.getVendorService() == null) {
            context.setVendorService(InternalVendorService.getInstance(applicationContext));
        }

        if (context.getHttpRequestLog() == null) {
            LogContext logContext = LogContextHolder.get();
            context.setHttpRequestLog(LogContextService.toHttpRequestLog(logContext));
        }
    }

    /**
     * If settled bet retrieval is required, prepareSettledBets must be successful
     * in order to proceed with the rollback.
     * OR
     * If settled bet retrieval is NOT required, just proceed with rollback.
     */
    private void processRollbackSettledBets(BetRollbackContext context) {
        LogContext logContext = LogContextHolder.get();
        logContext.setLogGroup("Rollback");

        if (!context.isRetrieveSettledBet() || settledBetDataService.prepareSettledBets(context.getBetId(), context.getTimestamp())) {
            processRollbackTransaction(context, context.getGameSession(), logContext);
        }
    }

    private void processRollbackTransaction(
            BetRollbackContext context,
            GameSession gameSession,
            LogContext logContext) {

        HttpRequestLog httpRequestLog = context.getHttpRequestLog();
        try {
            walletService.processRollback(
                    httpRequestLog.getId(),
                    rollbackDataMapper.toRollbackData(context),
                    gameSession,
                    context.getVendorService(),
                    httpRequestLog
            );

        } catch (Exception ex) {
            walletExceptionTranslator.translateAndThrow(ex);

        } finally {
            cleanup();
            LogContextService.updateLogContextFromHttpRequestLog(logContext, httpRequestLog);
        }
    }

    private void cleanup() {
        stateHolder.remove();
    }

    private static class InternalVendorService extends BaseVendorService {
        /**
         * Temporary factory method for InternalVendorService during BaseVendorService deprecation.
         * TODO: Remove this class once BaseVendorService is fully deprecated.
         */
        public static InternalVendorService getInstance(ApplicationContext applicationContext) {
            InternalVendorService vendorService = new InternalVendorService();
            // due to BaseVendorService field level autowired, manual autowire dependencies are required.
            applicationContext.getAutowireCapableBeanFactory().autowireBean(vendorService);
            return vendorService;
        }
    }
}
