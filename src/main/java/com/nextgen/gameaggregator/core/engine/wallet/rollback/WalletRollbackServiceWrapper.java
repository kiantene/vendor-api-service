package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.entity.warehouse.BetHistory;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.SettledBetService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.service.WarehouseBetHistoryService;
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

        CompletableFuture.runAsync(() -> {
            try {
                this.rollbackSettledBet(context, gameSession, vendorService, httpRequestLog);
            } catch (Exception e) {
                // TODO: handle exceptions
                e.printStackTrace();
            } finally {
                // cleanup

            }
        }, CompletableFuture.delayedExecutor(delaySeconds, TimeUnit.MILLISECONDS));
    }

    private void rollbackSettledBet(BetRollbackContext context, GameSession gameSession, BaseVendorService vendorService, HttpRequestLog httpRequestLog) throws Exception {
        //TODO BETHISTORY SHOULD BE A LIST
        List<BetHistory> betHistoryList = this.findSettledBet(context);

        //TODO INSERT AS LIST TO COUCHBASE SETTLEDBET
        if (betHistoryList == null || betHistoryList.isEmpty()) return;

        List<SettledBet> settledBetList = this.buildSettledBetDocument(betHistoryList);
        storeSettledBetDocument(settledBetList);

        //TODO ADD CONDITION TO CHECK IF DONT HAVE ROLLBACK SETTLEDBET THEN ONLY DO MAPPING AND DO PROCESSROLLBACK
        RollbackData rollbackData = rollbackDataMapper.toRollbackData(context);
        walletService.processRollback(context.getTraceId(), rollbackData, gameSession, vendorService, httpRequestLog);
    }

    private List<BetHistory> findSettledBet(BetRollbackContext context) {
        // TODO: search bet history from clickhouse
        return warehouseBetHistoryService.findByExternalTransactionIdAndVendorSettleTime(context.getBetId(), context.getTimestamp());
    }

    private List<SettledBet> buildSettledBetDocument(List<BetHistory> betHistoryList) {
        // TODO: build settled bet document from bet history
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
            // TODO: save settledBet document back to couchbase
            settledBetService.save(settledBetList);
        } catch (Exception ex) {
            // TODO: store failure in couchbase rollback_dlq
        }
    }
}
