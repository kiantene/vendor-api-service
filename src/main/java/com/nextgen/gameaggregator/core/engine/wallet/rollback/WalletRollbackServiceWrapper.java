package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.entity.warehouse.BetHistory;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class WalletRollbackServiceWrapper {
    private final HttpService httpService;
    private final WalletService walletService;
    private final WarehouseBetHistoryService warehouseBetHistoryService;
    private final SettledBetService settledBetService;
    private final RollbackDataMapper rollbackDataMapper;

    public void asyncRollbackSettledBet(BetRollbackContext context) {
        asyncRollbackSettledBet(context, 1000);
    }

    public void asyncRollbackSettledBet(BetRollbackContext context, long delaySeconds) {
        GameSession gameSession = context.getGameSession();
        BaseVendorService vendorService = context.getVendorService();
        HttpRequestLog httpRequestLog = context.getHttpRequestLog();
        HttpServletRequest httpServletRequest = context.getHttpServletRequest();
        BetRollbackVo betRollbackVo = new BetRollbackVo();

        HttpRequestLog newHttpRequestLog = httpService.start(httpServletRequest);
        CompletableFuture.runAsync(() -> {
            newHttpRequestLog.setUrl(httpRequestLog.getUrl() + " (Internal use)");
            newHttpRequestLog.setRequestBody(httpRequestLog.getRequestBody());
            try {
                this.rollbackSettledBet(context, gameSession, vendorService, newHttpRequestLog);
                betRollbackVo.setStatus("SUCCESS");
            } catch (Exception e) {
                betRollbackVo.setStatus("FAILED");
                httpService.logError(newHttpRequestLog, e);
            } finally {
                httpService.end(newHttpRequestLog, betRollbackVo);
            }
        }, CompletableFuture.delayedExecutor(delaySeconds, TimeUnit.MILLISECONDS));
    }

    private void rollbackSettledBet(BetRollbackContext context, GameSession gameSession, BaseVendorService vendorService, HttpRequestLog httpRequestLog) throws Exception {
        List<BetHistory> betHistoryList = this.findSettledBet(context);

        if (betHistoryList == null || betHistoryList.isEmpty())
            throw new BetNotFoundException("betHistoryList null or empty");

        List<SettledBet> settledBetList = this.buildSettledBetDocument(betHistoryList);
        this.storeSettledBetDocument(settledBetList);

        RollbackData rollbackData = rollbackDataMapper.toRollbackData(context);
        walletService.processRollback(httpRequestLog.getId(), rollbackData, gameSession, vendorService, httpRequestLog);
    }

    private List<BetHistory> findSettledBet(BetRollbackContext context) {
        return warehouseBetHistoryService.findByExternalTransactionIdAndVendorSettleTime(context.getBetId(), context.getTimestamp());
    }

    private List<SettledBet> buildSettledBetDocument(List<BetHistory> betHistoryList) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        Long createTime = System.currentTimeMillis();

        return betHistoryList.stream()
                .map(betHistory -> {
                    SettledBet settledBet = modelMapper.map(betHistory, SettledBet.class);
                    settledBet.setBetId(betHistory.getId());
                    settledBet.setCreateTime(createTime);
                    return settledBet;
                })
                .toList();
    }

    private void storeSettledBetDocument(List<SettledBet> settledBetList) {
        try {
            settledBetService.saveAll(settledBetList);
        } catch (Exception ex) {
            // TODO: store failure in couchbase rollback_dlq
            throw ex;
        }
    }
}
