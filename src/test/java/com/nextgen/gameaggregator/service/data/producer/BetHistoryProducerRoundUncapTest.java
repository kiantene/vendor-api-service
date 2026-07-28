package com.nextgen.gameaggregator.service.data.producer;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.entity.VendorCurrency;
import com.nextgen.gameaggregator.core.service.AgentDataService;
import com.nextgen.gameaggregator.core.service.AgentPlayerDataService;
import com.nextgen.gameaggregator.core.service.GameCategoryDataService;
import com.nextgen.gameaggregator.core.service.VendorCurrencyDataService;
import com.nextgen.gameaggregator.core.service.VendorDataService;
import com.nextgen.gameaggregator.entity.couchbase.AgentMeta;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.entity.ga.BetHistoryUncap;
import com.nextgen.gameaggregator.entity.ga.BetHistoryV3;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.enums.TxnType;
import com.nextgen.gameaggregator.service.CurrencyConversionService;
import com.nextgen.gameaggregator.service.KafkaService;
import com.nextgen.gameaggregator.service.VendorPlayerService;
import com.nextgen.gameaggregator.service.WarehouseBetHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the OVI-2391 uncap-emission decision in {@link BetHistoryProducer#publishBetHistoryByRound}
 * (the SettleByRound legacy path): the uncap record is produced when the round was capped, and not
 * otherwise.
 */
class BetHistoryProducerRoundUncapTest {

    private KafkaService kafkaService;
    private BetHistoryMapper betHistoryMapper;
    private VendorCurrencyDataService vendorCurrencyService;
    private BetHistoryProducer producer;

    @BeforeEach
    void setUp() {
        kafkaService = mock(KafkaService.class);
        betHistoryMapper = mock(BetHistoryMapper.class);
        vendorCurrencyService = mock(VendorCurrencyDataService.class);

        producer = new BetHistoryProducer(
                mock(CurrencyConversionService.class),
                kafkaService,
                mock(AgentDataService.class),
                mock(AgentPlayerDataService.class),
                mock(GameCategoryDataService.class),
                vendorCurrencyService,
                mock(VendorPlayerService.class),
                mock(VendorDataService.class),
                betHistoryMapper,
                mock(BetTxnToBetHistoryMapper.class),
                mock(WarehouseBetHistoryService.class)
        );

        when(betHistoryMapper.initialise(any(), any(), any())).thenReturn(new BetHistoryV3());
        VendorCurrency vc = mock(VendorCurrency.class);
        when(vc.getFromVendorRate()).thenReturn(BigDecimal.ONE);
        when(vendorCurrencyService.getByVendorIdAndCurrencyId(any(), any())).thenReturn(vc);
    }

    private BetResultContext context() {
        return BetResultContext.builder().vendorId(134).currencyId(7).gameCategoryId(1).build();
    }

    private RoundTxn betSlice() {
        RoundTxn t = new RoundTxn();
        t.setType(TxnType.BET);
        t.setStatus(TxnStatus.SUCCESS);
        t.setState(GameRoundState.UNSETTLED);
        t.setBetAmount(new BigDecimal("2167"));
        return t;
    }

    private RoundTxn resultSlice(BigDecimal win, BigDecimal cappedWin) {
        RoundTxn t = new RoundTxn();
        t.setType(TxnType.RESULT);
        t.setStatus(TxnStatus.SUCCESS);
        t.setState(GameRoundState.SETTLED);
        t.setWinAmount(win);
        t.setCappedWinAmount(cappedWin);
        return t;
    }

    private GameRound round(RoundTxn result) {
        GameRound round = new GameRound();
        AgentMeta meta = new AgentMeta();
        meta.setAgentId(1116);
        round.setAgentMeta(meta);
        round.setTransactions(List.of(betSlice(), result));
        return round;
    }

    @Test
    void emitsUncap_whenRoundWasCapped() {
        GameRound round = round(resultSlice(new BigDecimal("2100"), new BigDecimal("2000")));

        producer.publishBetHistoryByRound(context(), round, new GameTransaction(), new GameTransaction());

        verify(kafkaService, times(1)).produceBetHistoryV3(any());
        verify(kafkaService, times(1)).produceBetHistoryUncap(any(BetHistoryUncap.class));
    }

    @Test
    void doesNotEmitUncap_whenRoundNotCapped() {
        GameRound round = round(resultSlice(new BigDecimal("2100"), null));

        producer.publishBetHistoryByRound(context(), round, new GameTransaction(), new GameTransaction());

        verify(kafkaService, times(1)).produceBetHistoryV3(any());
        verify(kafkaService, never()).produceBetHistoryUncap(any());
    }
}
