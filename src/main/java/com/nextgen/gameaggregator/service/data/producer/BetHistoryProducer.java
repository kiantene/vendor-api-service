package com.nextgen.gameaggregator.service.data.producer;

import com.nextgen.gameaggregator.core.entity.AgentPlayer;
import com.nextgen.gameaggregator.core.service.AgentPlayerDataService;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.entity.ga.custom.WarehouseFutureEntity;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.service.CurrencyConversionService;
import com.nextgen.gameaggregator.service.KafkaService;
import com.nextgen.gameaggregator.service.VendorPlayerService;
import com.nextgen.gameaggregator.service.WarehouseBetHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BetHistoryProducer {
    private final CurrencyConversionService currencyConversionService;
    private final KafkaService kafkaService;
    private final AgentPlayerDataService agentPlayerDataService;
    private final VendorPlayerService vendorPlayerService;
    private final WarehouseBetHistoryService warehouseBetHistoryService;

    public void publish(SettledBet settledBet,
                        String productCode,
                        Integer productId,
                        Integer productGameId,
                        String agentPlayerUsername,
                        String vendorPlayerUsername,
                        BigDecimal fromVendorConversionRate,
                        boolean requirePreprocessing) {

        BetHistory betHistory = new BetHistory(settledBet);

        if (!requirePreprocessing) {
            produceBetHistory(
                    betHistory,
                    settledBet,
                    productCode,
                    productId,
                    productGameId,
                    agentPlayerUsername,
                    vendorPlayerUsername,
                    fromVendorConversionRate
            );
        } else {
            kafkaService.producePreprocessingBetHistory(
                    betHistory,
                    agentPlayerUsername,
                    vendorPlayerUsername,
                    fromVendorConversionRate
            );
        }
    }

    public BetHistoryV3 prepareBetHistoryV3(BetHistory betHistory,
                                            String productCode,
                                            Integer productId,
                                            Integer productGameId,
                                            String agentPlayerUsername,
                                            String vendorPlayerUsername) {

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

    private String getVendorPlayerUsername(String username, Long vendorPlayerId) {
        if (username != null) return username;

        try {
            VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(vendorPlayerId, null);
            return vendorPlayer.getUsername();
        } catch (InvalidPlayerException ex) {
            return "";
        }
    }

    private void produceBetHistory(BetHistory betHistory,
                                   SettledBet settledBet,
                                   String productCode,
                                   Integer productId,
                                   Integer productGameId,
                                   String agentPlayerUsername,
                                   String vendorPlayerUsername,
                                   BigDecimal fromVendorConversionRate
    ) {

        currencyConversionService.doCurrencyConversionRateFromVendorForBetHistoryBeforeSendToKafka(betHistory, fromVendorConversionRate);

        if (betHistory.getGameSessionToken() == null) {
            betHistory.setGameSessionToken("");
        }

        if (agentPlayerUsername == null) {
            AgentPlayer agentPlayer = agentPlayerDataService.get(betHistory.getAgentPlayerId());
            agentPlayerUsername = agentPlayer.getUsername();
        }

        vendorPlayerUsername = getVendorPlayerUsername(vendorPlayerUsername, betHistory.getVendorPlayerId());

        BetHistoryV3 betHistoryV3 = prepareBetHistoryV3(
                betHistory,
                productCode,
                productId,
                productGameId,
                agentPlayerUsername,
                vendorPlayerUsername
        );

        kafkaService.produceBetHistoryV3(betHistoryV3);

        if (settledBet.getUncapWinAmount() != null) {
            BetHistoryUncap betHistoryUncap = BetHistoryUncap.copyOf(betHistoryV3);
            betHistoryUncap.setUncapWinAmount(settledBet.getUncapWinAmount());
            betHistoryUncap.setUncapJackpotAmount(settledBet.getUncapJackpotAmount());
            betHistoryUncap.setUncapWinLoss(settledBet.getUncapWinLoss());
            betHistoryUncap.setUncapEffectiveTurnover(settledBet.getUncapEffectiveTurnover());

            kafkaService.produceBetHistoryUncap(betHistoryUncap);
        }
    }
}
