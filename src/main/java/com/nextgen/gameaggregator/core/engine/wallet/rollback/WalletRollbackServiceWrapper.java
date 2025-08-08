package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.entity.warehouse.BetHistory;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.service.*;
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
    private static final long DEFAULT_DELAY_MILLISECONDS = 1000L;
    private final WalletService walletService;
    private final WarehouseBetHistoryService warehouseBetHistoryService;
    private final SettledBetService settledBetService;
    private final RollbackDataMapper rollbackDataMapper;
    private final ApplicationContext applicationContext;

    public void asyncRollbackSettledBet(BetRollbackContext context) {
        asyncRollbackSettledBet(context, DEFAULT_DELAY_MILLISECONDS);
    }

    public void asyncRollbackSettledBet(BetRollbackContext context, long delayMilliseconds) {
        validateContext(context);
        CompletableFuture.runAsync(
                () -> processAsyncRollback(context),
                CompletableFuture.delayedExecutor(delayMilliseconds, TimeUnit.MILLISECONDS)
        );
    }

    private void validateContext(BetRollbackContext context) {
        if (context == null) {
            throw new IllegalArgumentException("BetRollbackContext cannot be null");
        }
        if (context.getVendorService() == null) {
            InternalVendorService vendorService = new InternalVendorService();
            applicationContext.getAutowireCapableBeanFactory().autowireBean(vendorService);
            context.setVendorService(vendorService);
        }
    }

    private HttpRequestLog toHttpRequestLog(LogContext logContext) {
        final Integer PROCESSING = 1;

        HttpRequestLog httpRequestLog = new HttpRequestLog();
        logContext.setTraceId(httpRequestLog.getId());
        httpRequestLog.setUrl(logContext.getUrl());
        httpRequestLog.setRequestBody(logContext.getBody().toString());
        httpRequestLog.setStatus(PROCESSING);
        return httpRequestLog;
    }

    private void updateLogContext(LogContext logContext, HttpRequestLog httpRequestLog) {
        logContext.setStart(httpRequestLog.getBetStart());
        logContext.setEnd(httpRequestLog.getBetEnd());
        logContext.setApiStart(httpRequestLog.getOperatorStart());
        logContext.setApiEnd(httpRequestLog.getOperatorEnd());
        logContext.put(HttpRequestLog.class.getSimpleName(), httpRequestLog);
    }

    private void processAsyncRollback(BetRollbackContext context) {
        LogContext logContext = LogContextHolder.get();
        logContext.setLogGroup("Rollback");
        HttpRequestLog httpRequestLog = toHttpRequestLog(logContext);

        try {
            executeRollback(context, httpRequestLog);

        } catch (Exception e) {


        } finally {
            this.updateLogContext(logContext, httpRequestLog);
        }
    }

    private void executeRollback(BetRollbackContext context, HttpRequestLog httpRequestLog) throws Exception {
        List<BetHistory> betHistoryList = this.findSettledBet(context);

        if (betHistoryList == null || betHistoryList.isEmpty())
            throw new BetNotFoundException("betHistoryList null or empty");

        List<SettledBet> settledBetList = buildSettledBetDocuments(betHistoryList);
        storeSettledBetDocuments(settledBetList);

        processWalletRollback(context, httpRequestLog);
    }

    private List<BetHistory> findSettledBet(BetRollbackContext context) {
        return warehouseBetHistoryService.findByExternalTransactionIdAndVendorSettleTime(context.getBetId(), context.getTimestamp());
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

    private void processWalletRollback(BetRollbackContext context, HttpRequestLog httpRequestLog) throws Exception {
        walletService.processRollback(
                httpRequestLog.getId(),
                rollbackDataMapper.toRollbackData(context),
                context.getGameSession(),
                context.getVendorService(),
                httpRequestLog
        );
    }
}
