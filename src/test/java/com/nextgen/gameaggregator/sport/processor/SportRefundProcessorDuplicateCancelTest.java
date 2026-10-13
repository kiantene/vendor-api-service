package com.nextgen.gameaggregator.sport.processor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.sport.refund.SportRefundAction;
import com.nextgen.gameaggregator.service.BetResultRetryLogService;
import com.nextgen.gameaggregator.service.KafkaService;
import com.nextgen.gameaggregator.service.VendorCurrencyService;
import com.nextgen.gameaggregator.sport.entity.SportSettledBet;
import com.nextgen.gameaggregator.sport.service.SportSettledBetService;
import com.nextgen.gameaggregator.sport.service.SportUnsettledBetService;

/**
 * ONEAPI-974 — ordering of the two idempotency checks in the refund path.
 *
 * <p>A successful refund DELETES the unsettled bet, so a duplicate cancel always arrives with nothing in
 * sport_unsettled_bet. The settled-bet check must therefore run BEFORE absence is treated as not-found.
 *
 * <p>This matters because CancelBetAction's idempotent marker is operation-scoped: if one txn in a batch
 * fails, the marker is cleared for the whole operation and SABA's resend re-drives the siblings that
 * already refunded. Those resends are only safe because they land on
 * BetResultIdempotentViolationException here.
 */
class SportRefundProcessorDuplicateCancelTest {

    private static final String USER = "32msz0fb9xe";
    private static final String BET_ID = "1349703";
    private static final String EXT_TXN_ID = "op-1_1349703";

    private SportUnsettledBetService sportUnsettledBetService;
    private SportSettledBetService sportSettledBetService;
    private SportRefundAction sportRefundAction;
    private VendorCurrencyService vendorCurrencyService;
    private WalletRequestService walletRequestService;

    private SportRefundProcessor processor;

    @BeforeEach
    void setUp() {
        sportUnsettledBetService = mock(SportUnsettledBetService.class);
        sportSettledBetService = mock(SportSettledBetService.class);
        sportRefundAction = mock(SportRefundAction.class);
        vendorCurrencyService = mock(VendorCurrencyService.class);
        walletRequestService = mock(WalletRequestService.class);

        processor = new SportRefundProcessor(sportUnsettledBetService, sportSettledBetService,
                sportRefundAction, mock(KafkaService.class), vendorCurrencyService,
                mock(BetResultRetryLogService.class), walletRequestService);
    }

    private WalletRequest refundRequest() {
        WalletRequest walletRequest = new WalletRequest();
        walletRequest.setTraceId("trace-2");
        walletRequest.setVendorPlayerUsername(USER);
        walletRequest.setExternalTransactionId(EXT_TXN_ID);
        walletRequest.setVendorBetId(BET_ID);
        walletRequest.setTimestamp(System.currentTimeMillis());
        walletRequest.setVendorId(40);
        return walletRequest;
    }

    @Test
    void duplicateCancelOfAnAlreadyRefundedBet_isIdempotent_notBetNotFound() throws Exception {
        // The first refund succeeded, so the unsettled bet is gone and a settled bet remains.
        when(sportUnsettledBetService.awaitPlaceBetOutcome(anyString(), anyString()))
                .thenReturn(Optional.empty());

        SportSettledBet alreadyRefunded = new SportSettledBet();
        alreadyRefunded.setExternalTransactionId(EXT_TXN_ID);
        alreadyRefunded.setStatus(ResponseCodes.Status.SC_OK.code);
        when(sportSettledBetService.getByVendorPlayerUsernameAndVendorBetId(USER, BET_ID))
                .thenReturn(alreadyRefunded);

        assertThrows(BetResultIdempotentViolationException.class,
                () -> processor.process(refundRequest()),
                "A resent cancel for a bet that already refunded must be rejected as idempotent. Throwing "
                        + "BetNotFoundException instead would let SABA resend forever, each attempt burning "
                        + "the full in-flight wait.");

        // And it must never reach the operator a second time.
        verify(sportRefundAction, never()).callToOperator(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void cancelForABetThatNeverExisted_isStillBetNotFound() throws Exception {
        when(sportUnsettledBetService.awaitPlaceBetOutcome(anyString(), anyString()))
                .thenReturn(Optional.empty());

        // Nothing in sport_settled_bet either -> this really is an unknown bet.
        when(sportSettledBetService.getByVendorPlayerUsernameAndVendorBetId(USER, BET_ID))
                .thenThrow(new BetNotFoundException("no settled bet"));

        assertThrows(BetNotFoundException.class, () -> processor.process(refundRequest()));

        verify(sportRefundAction, never()).callToOperator(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }
}
