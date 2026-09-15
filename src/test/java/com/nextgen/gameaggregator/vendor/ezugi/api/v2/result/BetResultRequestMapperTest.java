package com.nextgen.gameaggregator.vendor.ezugi.api.v2.result;

import com.nextgen.gameaggregator.core.engine.wallet.BetTransaction;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.enums.TxnType;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import com.nextgen.gameaggregator.vendor.ezugi.constant.EndPoints;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BetResultRequestMapperTest {

    @Mock
    private GameTransactionService gameTransactionService;

    private BetResultRequestMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new BetResultRequestMapper(gameTransactionService);
    }

    @Test
    @DisplayName("BC Shim: Should resolve parent context and all per-bet roundIds to debitTransactionId when legacy roundId == debitTransactionId")
    void toInternal_legacyInFlightDebit_resolvesRoundIdToDebitTransactionId() {
        // Distinct non-equal fixtures (Equal-value fixtures prove nothing)
        String legacyDebitTxnId = "DEBIT-LEGACY-TXN-1001";
        Long requestRoundId = 999999L; // String.valueOf(999999L) != "DEBIT-LEGACY-TXN-1001"

        BetResultRequest request = buildSampleRequest(legacyDebitTxnId, requestRoundId);

        // Simulate legacy GameTransaction stored in DB where roundId == debitTransactionId
        GameTransaction legacyTxn = new GameTransaction();
        legacyTxn.setRoundId(legacyDebitTxnId);
        legacyTxn.setVendorBetId(legacyDebitTxnId);

        String expectedDocId = GameTransaction.createDocId(EndPoints.VENDOR, TxnType.BET, legacyDebitTxnId);
        when(gameTransactionService.get(expectedDocId)).thenReturn(Optional.of(legacyTxn));

        // Execute mapping
        BetResultContext resultContext = mapper.toInternal(request);

        // 1. Assert parent credit BetResultContext.roundId resolves to debitTransactionId
        assertThat(resultContext.getRoundId())
                .as("Parent BetResultContext roundId must resolve to legacy debitTransactionId")
                .isEqualTo(legacyDebitTxnId)
                .isNotEqualTo(String.valueOf(requestRoundId));

        // 2. Assert every per-bet BetTransaction.roundId resolves to debitTransactionId
        List<BetTransaction> betTransactions = resultContext.getBetTransactions();
        assertThat(betTransactions)
                .as("BetTransactions list must not be empty")
                .isNotEmpty();

        assertThat(betTransactions)
                .allSatisfy(betTxn -> {
                    assertThat(betTxn.getRoundId())
                            .as("Per-bet BetTransaction roundId must resolve to legacy debitTransactionId")
                            .isEqualTo(legacyDebitTxnId)
                            .isNotEqualTo(String.valueOf(requestRoundId));
                });
    }

    @Test
    @DisplayName("Post-Deployment: Should resolve parent context and all per-bet roundIds to vendorRoundId when no legacy debit record matches")
    void toInternal_postDeploymentDebit_resolvesRoundIdToVendorRoundId() {
        // Distinct non-equal fixtures
        String debitTxnId = "DEBIT-NEW-TXN-2002";
        Long requestRoundId = 777777L;

        BetResultRequest request = buildSampleRequest(debitTxnId, requestRoundId);

        // Simulate post-deployment GameTransaction where roundId == vendorRoundId ("777777")
        GameTransaction postDeploymentTxn = new GameTransaction();
        postDeploymentTxn.setRoundId(String.valueOf(requestRoundId));
        postDeploymentTxn.setVendorBetId(debitTxnId);

        String expectedDocId = GameTransaction.createDocId(EndPoints.VENDOR, TxnType.BET, debitTxnId);
        when(gameTransactionService.get(expectedDocId)).thenReturn(Optional.of(postDeploymentTxn));

        // Execute mapping
        BetResultContext resultContext = mapper.toInternal(request);

        // 1. Assert parent credit BetResultContext.roundId resolves to request vendorRoundId
        assertThat(resultContext.getRoundId())
                .as("Parent BetResultContext roundId must resolve to vendorRoundId")
                .isEqualTo(String.valueOf(requestRoundId))
                .isNotEqualTo(debitTxnId);

        // 2. Assert every per-bet BetTransaction.roundId resolves to request vendorRoundId
        List<BetTransaction> betTransactions = resultContext.getBetTransactions();
        assertThat(betTransactions).isNotEmpty();
        assertThat(betTransactions)
                .allSatisfy(betTxn -> {
                    assertThat(betTxn.getRoundId())
                            .as("Per-bet BetTransaction roundId must resolve to vendorRoundId")
                            .isEqualTo(String.valueOf(requestRoundId))
                            .isNotEqualTo(debitTxnId);
                });
    }

    private BetResultRequest buildSampleRequest(String debitTxnId, Long roundId) {
        BetResultRequest request = new BetResultRequest();
        request.setDebitTransactionId(debitTxnId);
        request.setRoundId(BigInteger.valueOf(roundId));
        request.setTransactionId("CREDIT-TXN-5005");
        request.setCreditAmount(new BigDecimal("100.00"));
        request.setTableId(101);
        request.setCurrency("EUR");
        request.setUid("player123");
        request.setEndRound(true);

        BetResultRequest.GameDataString gameData = new BetResultRequest.GameDataString();
        gameData.setBetAmount(new BigDecimal("10.00"));

        BetResultRequest.Bet bet1 = new BetResultRequest.Bet();
        bet1.setBetName("MainBet");
        bet1.setBetAmount(new BigDecimal("10.00"));

        gameData.setBetsList(List.of(bet1));
        gameData.setWinningBets(Map.of("MainBet", new BigDecimal("100.00")));
        request.setGameDataString(gameData);

        return request;
    }
}
