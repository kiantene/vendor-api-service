package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.WalletExceptionTranslator;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.entity.warehouse.BetHistory;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.SettledBetService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.service.WarehouseBetHistoryService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class WalletRollbackServiceWrapper {
    private static class InternalVendorService extends BaseVendorService {}
    private static final ThreadLocal<BetRollbackWrapperContext> stateHolder = new ThreadLocal<>();
    private static final long DEFAULT_DELAY_MILLISECONDS = 1000L;
    private final ApplicationContext applicationContext;
    private final WalletService walletService;
    private final WarehouseBetHistoryService warehouseBetHistoryService;
    private final SettledBetService settledBetService;
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
            // retrieve gameSession based on vendorToken
            // if not found, create new game session based on vendorPlayerUsername
        }

        if (context.getVendorService() == null) {
            InternalVendorService vendorService = new InternalVendorService();
            // due to BaseVendorService field level autowired, manual autowire dependencies are required.
            applicationContext.getAutowireCapableBeanFactory().autowireBean(vendorService);
            context.setVendorService(vendorService);
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

        if (!context.isRetrieveSettledBet() || prepareSettledBets(context)) {
            processRollbackTransaction(context, context.getGameSession(), logContext);
        }
    }

    private boolean prepareSettledBets(BetRollbackContext context) {
        List<BetHistory> betHistoryList = this.findSettledBets(context);
        if (betHistoryList == null || betHistoryList.isEmpty()) return false;

        List<SettledBet> settledBetList = buildSettledBetDocuments(betHistoryList);
        storeSettledBetDocuments(settledBetList);

        return true;
    }

    private List<BetHistory> findSettledBets(BetRollbackContext context) {
        return warehouseBetHistoryService
                .findByExternalTransactionIdAndVendorSettleTime(
                        context.getBetId(),
                        context.getTimestamp()
                );
    }

    private List<SettledBet> buildSettledBetDocuments(List<BetHistory> betHistoryList) {
        long createTime = System.currentTimeMillis();

        return betHistoryList.stream()
                .map(betHistory -> mapToSettledBet(betHistory, createTime))
                .toList();
    }

    private SettledBet mapToSettledBet(BetHistory betHistory, long createTime) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        SettledBet settledBet = modelMapper.map(betHistory, SettledBet.class);
        settledBet.setBetId(betHistory.getId());
        settledBet.setCreateTime(createTime);
        return settledBet;
    }

    private void storeSettledBetDocuments(List<SettledBet> settledBetList) {
        try {
            settledBetService.saveAll(settledBetList);
        } catch (Exception ex) {
            // TODO: store failure in couchbase rollback_dlq
            throw ex;
        }
    }

    private void processRollbackTransaction(
            BetRollbackContext context,
            GameSession gameSession,
            LogContext logContext) {

        HttpRequestLog httpRequestLog = LogContextService.toHttpRequestLog(logContext);
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
}
