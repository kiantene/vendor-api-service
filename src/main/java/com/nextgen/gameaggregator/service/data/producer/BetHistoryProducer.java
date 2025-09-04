package com.nextgen.gameaggregator.service.data.producer;

import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.entity.ga.custom.WarehouseFutureEntity;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.service.business.maxpayout.PayoutCapResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BetHistoryProducer {
    private final CurrencyConversionService currencyConversionService;
    private final KafkaService kafkaService;
    private final AgentPlayerService agentPlayerService;
    private final VendorPlayerService vendorPlayerService;
    private final WarehouseBetHistoryService warehouseBetHistoryService;

    public void publish(BetHistory betHistory,
                        String productCode,
                        Integer productId,
                        Integer productGameId,
                        String agentPlayerUsername,
                        String vendorPlayerUsername,
                        BigDecimal fromVendorConversionRate,
                        PayoutCapResult payoutCapResult) {

        currencyConversionService.doCurrencyConversionRateFromVendorForBetHistoryBeforeSendToKafka(betHistory, fromVendorConversionRate);

        if (betHistory.getGameSessionToken() == null) {
            betHistory.setGameSessionToken("");
        }

        if (agentPlayerUsername == null) {
            AgentPlayer agentPlayer = agentPlayerService.get(betHistory.getAgentPlayerId());
            agentPlayerUsername = agentPlayer.getUsername();
        }

        if (vendorPlayerUsername == null) {
            VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(betHistory.getVendorPlayerId(), null);
            vendorPlayerUsername = vendorPlayer.getUsername();
        }

        BetHistoryV3 betHistoryV3 = prepareBetHistoryV3(
                betHistory,
                productCode,
                productId,
                productGameId,
                agentPlayerUsername,
                vendorPlayerUsername
        );

        kafkaService.produceBetHistoryV3(betHistoryV3);

        BetHistoryUncap betHistoryUncap = BetHistoryUncap.copyOf(betHistoryV3);
        betHistoryUncap.setUncapWinAmount(payoutCapResult.uncapWinAmount());
        betHistoryUncap.setUncapJackpotAmount(payoutCapResult.uncapJackpotAmount());
        betHistoryUncap.setUncapWinLoss(payoutCapResult.uncapWinLoss());
        betHistoryUncap.setUncapEffectiveTurnover(payoutCapResult.uncapEffectiveTurnover());

        kafkaService.produceBetHistoryUncap(betHistoryUncap);
    }

    public BetHistoryV3 prepareBetHistoryV3(BetHistory betHistory,
                                            String productCode,
                                            Integer productId,
                                            Integer productGameId,
                                            String agentPlayerUsername,
                                            String vendorPlayerUsername
                                            ) {

        WarehouseFutureEntity warehouseFutureEntity = this.getFutureEntityForBetHistory(betHistory);

        return new BetHistoryV3(
                betHistory,
                productCode,
                productId,
                productGameId,
                agentPlayerUsername,
                vendorPlayerUsername,
                warehouseFutureEntity
        );
    }

    private WarehouseFutureEntity getFutureEntityForBetHistory(BetHistory betHistory) {
        return warehouseBetHistoryService.getWarehouseBetHistoryInfoCache(betHistory.getVendorGameId(), betHistory.getVendorId(), betHistory.getGameCategoryId(),
                betHistory.getCurrencyId(), betHistory.getAgentId());
    }
}
