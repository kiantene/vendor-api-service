package com.nextgen.gameaggregator.operator.wallet.bet;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.entity.ga.WalletTransaction;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.repository.ga.writer.WalletTransactionRepository;
import com.nextgen.gameaggregator.service.WalletTransactionService;
import com.nextgen.gameaggregator.service.WalletTransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WalletTransactionServiceTest {

    @Mock
    private WalletTransactionRepository walletTransactionRepository;
    private WalletTransactionService walletTransactionService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);  // Initialize mocks
        this.walletTransactionService = new WalletTransactionServiceImpl(walletTransactionRepository);

    }

    @Test
    void testWalletTransactionServiceCreate() throws InvalidRequestException {
        WalletRequest walletRequest = new WalletRequest();
        String traceId = "traceIdA";

        walletRequest.setVendorPlayerUsername("johndoeA");
        walletRequest.setToken(UUID.randomUUID().toString());
        walletRequest.setVendorGameCode("vendorGameCodeA");
        walletRequest.setCurrencyId(1);
        walletRequest.setTransactionId(traceId);
        walletRequest.setBetId(traceId);
        walletRequest.setVendorId(1);
        walletRequest.setExternalTransactionId("externalTransactionIdA");
        walletRequest.setVendorBetId("vendorBetIdA");
        walletRequest.setRoundId("vendorRoundIdA");
        walletRequest.setTakeAll(0);
        walletRequest.setTransferAmount(BigDecimal.valueOf(1000));
        walletRequest.setTimestamp(System.currentTimeMillis());

        WalletTransaction walletTransaction = walletTransactionService.prepareEntity(walletRequest, OperatorWalletService.DEBIT);

        String walletRequestId = walletTransaction.getExternalTransactionId() + "_" + walletTransaction.getAction() + "_" +
                walletTransaction.getVendorPlayerUsername() + "_" + walletTransaction.getVendorGameCode();

        assertEquals(walletRequestId, walletTransaction.getId());
        assertEquals(BigDecimal.ZERO, walletTransaction.getBalance());
        assertEquals(0, walletTransaction.getOperatorStatus());
        assertEquals(true, walletTransaction.getCreatedDate() != null);
        assertEquals(walletRequest.getVendorId(), walletTransaction.getVendorId());
        assertEquals(walletRequest.getVendorPlayerUsername(), walletTransaction.getVendorPlayerUsername());
        assertEquals(walletRequest.getToken(), walletTransaction.getToken());
        assertEquals(walletRequest.getVendorGameCode(), walletTransaction.getVendorGameCode());
        assertEquals(walletRequest.getCurrencyId(), walletTransaction.getCurrencyId());
        assertEquals(walletRequest.getTransactionId(), walletTransaction.getTransactionId());
        assertEquals(walletRequest.getBetId(), walletTransaction.getBetId());
        assertEquals(walletRequest.getExternalTransactionId(), walletTransaction.getExternalTransactionId());
        assertEquals(walletRequest.getVendorBetId(), walletTransaction.getVendorBetId());
        assertEquals(walletRequest.getRoundId(), walletTransaction.getRoundId());
        assertEquals(OperatorWalletService.DEBIT, walletTransaction.getAction());
        assertEquals(walletRequest.getTakeAll(), walletTransaction.getTakeAll());
        assertEquals(walletRequest.getTransferAmount(), walletTransaction.getTransferAmount());
        assertEquals(walletRequest.getTimestamp(), walletTransaction.getTimestamp());
    }

    @Test
    void testWalletTransactionServiceCreateWithExceptionIsThrown() {

        WalletRequest walletRequest = new WalletRequest();
        String traceId = "traceIdA";

        walletRequest.setVendorPlayerUsername("johndoeA");
        walletRequest.setToken(UUID.randomUUID().toString());
        walletRequest.setVendorGameCode("vendorGameCodeA");
        walletRequest.setCurrencyId(1);
        walletRequest.setVendorId(1);
        walletRequest.setTransactionId(traceId);
        walletRequest.setBetId(traceId);
        walletRequest.setExternalTransactionId("externalTransactionIdA");
        walletRequest.setVendorBetId("vendorBetIdA");
        walletRequest.setRoundId("vendorRoundIdA");
        walletRequest.setTakeAll(0);
        walletRequest.setTransferAmount(BigDecimal.valueOf(1000));
        walletRequest.setTimestamp(System.currentTimeMillis());

        Exception exception = assertThrows(InvalidRequestException.class, () -> {
            // Call the method that should throw the exception
            WalletTransaction walletTransaction = walletTransactionService.prepareEntity(walletRequest, OperatorWalletService.DEBIT);
        });

        String[] parts = exception.toString().split(":");
        String exceptionBeforeColon = parts[0].trim();

        assertEquals("com.nextgen.gameaggregator.exception.InvalidRequestException", exceptionBeforeColon);
    }

    @Test
    void testWalletTransactionServiceUpdateWalletRequestWithWalletTransaction() {
        WalletRequest walletRequest = new WalletRequest();
        walletRequest.setTransactionId("walletRequest.transactionId");
        walletRequest.setBetId("walletRequest.betId");
        walletRequest.setTimestamp(System.currentTimeMillis());

        WalletTransaction walletTransaction = new WalletTransaction();
        String traceId = "traceIdA";
        walletTransaction.setTransactionId(traceId);
        walletTransaction.setBetId(traceId);
        walletTransaction.setTimestamp(System.currentTimeMillis());

//        walletRequest = walletTransactionService.updateWalletRequestWithWalletTransaction(walletTransaction, walletRequest);

        assertEquals(walletRequest.getTransactionId(), walletTransaction.getTransactionId());
        assertEquals(walletRequest.getBetId(), walletTransaction.getBetId());
        assertEquals(walletRequest.getTimestamp(), walletTransaction.getTimestamp());
    }
}
