package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.engine.wallet.BetTransaction;
import com.nextgen.gameaggregator.entity.ga.BetHistory;
import com.nextgen.gameaggregator.service.KafkaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
@Deprecated(since = "max.payout", forRemoval = true)
public class WalletBetResultBatchService {

    private final BetTxnToBetHistoryMapper betTxnMapper;
    private final KafkaService kafkaService;

    @Async
    public CompletableFuture<Void> processBatch(List<BetTransaction> betTransactions, BetResultContext context) {

        try {
            log.info("Processing batch of {} transactions", betTransactions.size());

            List<BetHistory> betHistories = betTxnMapper.toBetHistoryList(betTransactions, context);

            betHistories.forEach(betHistory -> {
                try {
                    log.debug(new ObjectMapper().writeValueAsString(betHistory));
                    kafkaService.produceBetHistoryV3(
                            betHistory,
                            context.getProductCode(),
                            context.getProductId(),
                            context.getProductGameId(),
                            context.getAgentPlayerUsername(),
                            context.getVendorPlayerUsername(),
                            context.getFromVendorRate()
                    );
                    //TODO ADD kafkaService.produceBetHistoryUncap
                } catch (Exception e) {
                    log.error("Failed to send BetHistory to Kafka: {}", betHistory.getId(), e);
                    // Continue with other records
                }
            });

            log.info("Successfully processed batch of {} transactions", betHistories.size());

        } catch (Exception e) {
            log.error("Failed to process batch transactions", e);
        }

        return CompletableFuture.completedFuture(null);
    }
}
