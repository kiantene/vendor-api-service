package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.entity.ga.BetHistory;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class WalletRollbackServiceWrapper {
    private final WalletService walletService;
    private final RollbackDataMapper rollbackDataMapper;

    public void asyncRollbackSettledBet(BetRollbackContext context, long delaySeconds, GameSession gameSession, BaseVendorService vendorService, HttpRequestLog httpRequestLog) {
        CompletableFuture.runAsync(() -> {
            try {
                rollbackSettledBet(context, gameSession, vendorService, httpRequestLog);
            } catch (Exception e) {
                // TODO: handle exceptions
            } finally {
                // cleanup

            }
        }, CompletableFuture.delayedExecutor(delaySeconds, TimeUnit.SECONDS));
    }

    private void rollbackSettledBet(BetRollbackContext context, GameSession gameSession, BaseVendorService vendorService, HttpRequestLog httpRequestLog) throws Exception {
        BetHistory betHistory = findSettledBet(context);
        if (betHistory == null) return;

        SettledBet settledBetDocument = buildSettledBetDocument(betHistory);
        storeSettledBetDocument(settledBetDocument);
        RollbackData rollbackData = rollbackDataMapper.toRollbackData(context);

        walletService.processRollback(context.getTraceId(), rollbackData, gameSession, vendorService, httpRequestLog);
    }

    private BetHistory findSettledBet(BetRollbackContext context) {
        // TODO: search bet history from clickhouse

        return null;
    }

    private SettledBet buildSettledBetDocument(BetHistory betHistory) {
        // TODO: build settled bet document from bet history

        return null;
    }

    private void storeSettledBetDocument(SettledBet settledBet) {
        try {
            // TODO: save settledBet document back to couchbase
        } catch (Exception ex) {
            // TODO: store failure in couchbase rollback_dlq
        }
    }
}
