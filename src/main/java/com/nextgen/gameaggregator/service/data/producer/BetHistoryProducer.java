package com.nextgen.gameaggregator.service.data.producer;

import com.nextgen.gameaggregator.core.engine.wallet.BetTransaction;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BetHistoryProducer {
    private final CurrencyConversionService currencyConversionService;
    private final KafkaService kafkaService;
    private final AgentPlayerDataService agentPlayerDataService;
    private final VendorPlayerService vendorPlayerService;
    private final BetTxnToBetHistoryMapper betHistoryMapper;
    private final WarehouseBetHistoryService warehouseBetHistoryService;
    private record PlayerUsernames(String agentPlayer, String vendorPlayer) {}

    public void publish(SettledBet settledBet, BetHistoryPublishContext context) {
        publish(settledBet, context, BetResultConfig.ProcessingMode.SINGLE);
    }

    public void publish(SettledBet settledBet,
                        BetHistoryPublishContext context,
                        BetResultConfig.ProcessingMode processingMode) {

        BetHistory betHistory = new BetHistory(settledBet);
        PlayerUsernames usernames = getUsernamesIfNull(
                context,
                betHistory.getAgentPlayerId(),
                betHistory.getVendorPlayerId()
        );

        if (context.requirePreprocessing()) {
            kafkaService.producePreprocessingBetHistory(
                    betHistory,
                    usernames.agentPlayer(),
                    usernames.vendorPlayer(),
                    context.fromVendorRate()
            );
            return;
        }

        if (processingMode.isSingleMode()) {
            BetHistoryV3 betHistoryV3 = produceBetHistory(
                    betHistory,
                    context,
                    usernames
            );

            produceBetHistoryUncap(betHistoryV3, settledBet);
        } else {
            produceBetHistoryBatch(context.txnList(), settledBet, context, usernames);
            // TODO: to finalise solution for uncap bet history
        }
    }

    public BetHistoryV3 prepareBetHistoryV3(BetHistory betHistory,
                                            BetHistoryPublishContext context,
                                            PlayerUsernames usernames) {

        WarehouseFutureEntity warehouseFutureEntity = this.getFutureEntityForBetHistory(betHistory);

        return new BetHistoryV3(
                betHistory,
                context.productCode(),
                context.productId(),
                context.productGameId(),
                usernames.agentPlayer(),
                usernames.vendorPlayer(),
                warehouseFutureEntity
        );
    }

    private WarehouseFutureEntity getFutureEntityForBetHistory(BetHistory betHistory) {
        return warehouseBetHistoryService.getWarehouseBetHistoryInfoCache(
                betHistory.getVendorGameId(),
                betHistory.getVendorId(),
                betHistory.getGameCategoryId(),
                betHistory.getCurrencyId(),
                betHistory.getAgentId()
        );
    }

    private PlayerUsernames getUsernamesIfNull(BetHistoryPublishContext context,
                                               Long agentPlayerId,
                                               Long vendorPlayerId) {

        String agentPlayerUsername = context.agentPlayerUsername();
        if (agentPlayerUsername == null) {
            AgentPlayer agentPlayer = agentPlayerDataService.get(agentPlayerId);
            agentPlayerUsername = agentPlayer.getUsername();
        }

        String vendorPlayerUsername = context.vendorPlayerUsername();
        if (vendorPlayerUsername == null) {
            try {
                VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(vendorPlayerId, null);
                vendorPlayerUsername = vendorPlayer.getUsername();
            } catch (InvalidPlayerException ex) {
                vendorPlayerUsername = "";
            }
        }

        return new PlayerUsernames(agentPlayerUsername, vendorPlayerUsername);
    }

    private BetHistoryV3 produceBetHistory(BetHistory betHistory,
                                           BetHistoryPublishContext context,
                                           PlayerUsernames usernames) {

        currencyConversionService.doCurrencyConversionRateFromVendorForBetHistoryBeforeSendToKafka(
                betHistory,
                context.fromVendorRate()
        );

        if (betHistory.getGameSessionToken() == null) {
            betHistory.setGameSessionToken("");
        }

        BetHistoryV3 betHistoryV3 = prepareBetHistoryV3(betHistory, context, usernames);

        kafkaService.produceBetHistoryV3(betHistoryV3);

        return betHistoryV3;
    }

    private void produceBetHistoryUncap(BetHistoryV3 betHistoryV3, SettledBet settledBet) {
        if (settledBet.getUncapWinAmount() == null) return;

        BetHistoryUncap betHistoryUncap = BetHistoryUncap.copyOf(betHistoryV3);
        betHistoryUncap.setUncapWinAmount(settledBet.getUncapWinAmount());
        betHistoryUncap.setUncapJackpotAmount(settledBet.getUncapJackpotAmount());
        betHistoryUncap.setUncapWinLoss(settledBet.getUncapWinLoss());
        betHistoryUncap.setUncapEffectiveTurnover(settledBet.getUncapEffectiveTurnover());

        kafkaService.produceBetHistoryUncap(betHistoryUncap);
    }

    private void produceBetHistoryBatch(List<BetTransaction> betTransactions,
                                        SettledBet settledBet,
                                        BetHistoryPublishContext context,
                                        PlayerUsernames usernames) {

        if (betTransactions == null || betTransactions.isEmpty()) return;

        betHistoryMapper
                .toBetHistoryList(betTransactions, settledBet)
                .forEach(betHistory -> kafkaService.produceBetHistoryV3(
                        betHistory,
                        context.productCode(),
                        context.productId(),
                        context.productGameId(),
                        usernames.agentPlayer(),
                        usernames.vendorPlayer(),
                        context.fromVendorRate()
                ));
    }
}
