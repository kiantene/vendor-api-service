package com.nextgen.gameaggregator.operator.wallet.bet;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.operator.wallet.betdebit.WalletBetDebitDto;
import com.nextgen.gameaggregator.operator.wallet.betdebit.WalletBetDebitProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WalletBetDebitProcessorTest {

    private WalletBetDebitProcessor walletBetDebitProcessor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);  // Initialize mocks
//        this.walletBetDebitProcessor = new WalletBetDebitProcessor(walletRequestService, walletTransactionService);
        this.walletBetDebitProcessor = new WalletBetDebitProcessor(null, null);
    }

    @Test
    void testPrepareOperatorRequestData() throws InvalidRequestException {
        WalletRequest walletRequest = new WalletRequest();
        String traceId = "traceIdA";

        walletRequest.setTraceId(traceId);
        walletRequest.setTransactionId(traceId);
        walletRequest.setOperatorUsername("johndoeA");
        walletRequest.setExternalTransactionId("externalTransactionIdA");
        walletRequest.setTakeAll(0);
        walletRequest.setGameCode("GameCodeA");
        walletRequest.setCurrencyCode("USD");
        walletRequest.setRoundId("vendorRoundIdA");
        walletRequest.setTimestamp(System.currentTimeMillis());
        walletRequest.setToken(UUID.randomUUID().toString());
        walletRequest.setTransferAmount(BigDecimal.valueOf(1000));
        walletRequest.setFromVendorRate(BigDecimal.TEN);

        WalletBetDebitDto dto = walletBetDebitProcessor.prepareOperatorRequestData(walletRequest);

        assertEquals(walletRequest.getTraceId(), dto.getTraceId());
        assertEquals(walletRequest.getTransactionId(), dto.getTransactionId());
        assertEquals(walletRequest.getOperatorUsername(), dto.getUsername());
//        assertEquals(walletRequest.getExternalTransactionId(), dto.getExternalTransactionId());
//        assertEquals(walletRequest.getTakeAll(), dto.getTakeAll());
        assertEquals(walletRequest.getGameCode(), dto.getGameCode());
        assertEquals(walletRequest.getCurrencyCode(), dto.getCurrency());
        assertEquals(walletRequest.getRoundId(), dto.getRoundId());
        assertEquals(walletRequest.getTimestamp(), dto.getTimestamp());
        assertEquals(walletRequest.getToken(), dto.getToken());
        assertEquals(BigDecimal.valueOf(10000), dto.getAmount());
    }

    @Test
    void testPrepareOperatorRequestDataWithExceptionIsThrown() {
        WalletRequest walletRequest = new WalletRequest();
        String traceId = "traceIdA";

        walletRequest.setTraceId(traceId);
        walletRequest.setTransactionId(traceId);
        walletRequest.setOperatorUsername("johndoeA");
        walletRequest.setExternalTransactionId("externalTransactionIdA");
        //walletRequest.setTakeAll(0);
        walletRequest.setGameCode("GameCodeA");
        walletRequest.setCurrencyCode("USD");
        walletRequest.setRoundId("vendorRoundIdA");
        walletRequest.setTimestamp(System.currentTimeMillis());
        walletRequest.setToken(UUID.randomUUID().toString());
        walletRequest.setTransferAmount(BigDecimal.valueOf(1000));
        walletRequest.setFromVendorRate(BigDecimal.TEN);

        Exception exception = assertThrows(InvalidRequestException.class, () -> {
            // Call the method that should throw the exception
            WalletBetDebitDto dto = walletBetDebitProcessor.prepareOperatorRequestData(walletRequest);
        });

        String[] parts = exception.toString().split(":");
        String exceptionBeforeColon = parts[0].trim();

        assertEquals("com.nextgen.gameaggregator.exception.InvalidRequestException", exceptionBeforeColon);
    }
}
