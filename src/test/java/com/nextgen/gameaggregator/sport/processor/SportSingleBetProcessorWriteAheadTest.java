package com.nextgen.gameaggregator.sport.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.entity.ga.VendorCurrency;
import com.nextgen.gameaggregator.entity.ga.VendorGame;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.sport.bet.SportBetAction;
import com.nextgen.gameaggregator.service.KafkaService;
import com.nextgen.gameaggregator.service.VendorCurrencyService;
import com.nextgen.gameaggregator.service.VendorGameService;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBet;
import com.nextgen.gameaggregator.sport.service.SportUnsettledBetService;

/**
 * ONEAPI-974 — the bet must be durable BEFORE the operator is asked to move money.
 *
 * <p>Previously sport_unsettled_bet was only written after callToOperator returned, so for the whole
 * duration of a placebet the bet did not exist anywhere. A SABA cancel arriving in that window found
 * nothing and NPE'd. The Redis lock lease was never the root cause — it only set the width of the window.
 */
class SportSingleBetProcessorWriteAheadTest {

    private SportBetAction sportBetAction;
    private SportUnsettledBetService sportUnsettledBetService;
    private VendorCurrencyService vendorCurrencyService;
    private KafkaService kafkaService;
    private VendorGameService vendorGameService;

    private SportSingleBetProcessor processor;

    @BeforeEach
    void setUp() throws Exception {
        sportBetAction = mock(SportBetAction.class);
        sportUnsettledBetService = mock(SportUnsettledBetService.class);
        vendorCurrencyService = mock(VendorCurrencyService.class);
        kafkaService = mock(KafkaService.class);
        vendorGameService = mock(VendorGameService.class);

        processor = new SportSingleBetProcessor(sportBetAction, sportUnsettledBetService,
                vendorCurrencyService, kafkaService, vendorGameService);

        when(vendorCurrencyService.findByVendorIdAndCurrencyId(anyInt(), anyInt())).thenReturn(new VendorCurrency());
        when(vendorGameService.getByVendorGameId(anyInt())).thenReturn(new VendorGame());
    }

    private WalletRequest validBetRequest() {
        WalletRequest walletRequest = new WalletRequest();
        walletRequest.setTraceId("trace-1");
        walletRequest.setVendorPlayerUsername("32msz0fb9xe");
        walletRequest.setExternalTransactionId("op-1_1349703");
        walletRequest.setVendorBetId("1349703");
        walletRequest.setRoundId("1349703");
        walletRequest.setBetAmount(BigDecimal.TEN);
        walletRequest.setVendorId(40);
        walletRequest.setCurrencyId(1);
        walletRequest.setVendorGameId(1);
        return walletRequest;
    }

    @Test
    void betIsWrittenBeforeTheOperatorIsCalled() throws Exception {
        WalletRequest walletRequest = validBetRequest();
        when(sportBetAction.callToOperator(any(), any())).thenReturn(walletRequest);

        processor.process(walletRequest);

        InOrder inOrder = inOrder(sportUnsettledBetService, sportBetAction);
        inOrder.verify(sportUnsettledBetService).save(any(SportUnsettledBet.class));
        inOrder.verify(sportBetAction).callToOperator(any(), any());
    }

    @Test
    void theWriteAheadRecordIsMarkedInFlight_thenFlipsToOkOnceTheOperatorConfirms() throws Exception {
        WalletRequest walletRequest = validBetRequest();
        when(sportBetAction.callToOperator(any(), any())).thenReturn(walletRequest);

        // The processor mutates and re-saves the SAME instance, so an ArgumentCaptor would report the final
        // state for both saves. Snapshot operatorStatus at each call instead.
        List<Integer> operatorStatusPerSave = new ArrayList<>();
        doAnswer(invocation -> {
            SportUnsettledBet saved = invocation.getArgument(0);
            operatorStatusPerSave.add(saved.getOperatorStatus());
            return saved;
        }).when(sportUnsettledBetService).save(any(SportUnsettledBet.class));

        processor.process(walletRequest);

        assertEquals(2, operatorStatusPerSave.size(), "Expected a write-ahead save and a confirming save");

        // Write-ahead: "no outcome yet". Without this, awaitPlaceBetOutcome would read an in-flight bet as
        // finished and let the refund overtake the debit.
        assertEquals(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code, operatorStatusPerSave.get(0));

        // Confirming save: the debit is now known to have landed.
        assertEquals(ResponseCodes.Status.SC_OK.code, operatorStatusPerSave.get(1));
    }
}
