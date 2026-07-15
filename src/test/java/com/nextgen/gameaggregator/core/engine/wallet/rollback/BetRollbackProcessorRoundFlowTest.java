package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.core.api.ApiResult;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.retry.RetryQueueService;
import com.nextgen.gameaggregator.core.service.LegacyCleanupService;
import com.nextgen.gameaggregator.core.validator.ClientResponseValidator;
import com.nextgen.gameaggregator.core.vendor.config.VendorConfigService;
import com.nextgen.gameaggregator.core.webclient.ClientApiResponse;
import com.nextgen.gameaggregator.core.webclient.OperatorApiAdapter;
import com.nextgen.gameaggregator.core.webclient.OperatorApiRequest;
import com.nextgen.gameaggregator.entity.couchbase.AgentMeta;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.enums.TxnType;
import com.nextgen.gameaggregator.operator.wallet.rollback.WalletRollbackDto;
import com.nextgen.gameaggregator.service.business.GameRoundService;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import com.nextgen.gameaggregator.service.data.producer.BetHistoryProducer;
import com.nextgen.gameaggregator.service.data.producer.transactionhistory.BetTransactionHistoryProducer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers GA-14599 (#7): drives the full {@code processRollbackByRound} flow and asserts that EVERY
 * bet in the round gets its own rollback request carrying its own per-bet operator-POV meta — i.e.
 * the per-bet aggregation is actually wired through the round-rollback loop, not just the static
 * helper. (Gating to transfer wallet happens later in ClientRequestService and is covered there.)
 */
class BetRollbackProcessorRoundFlowTest {

    private static final int AGENT_ID = 100;

    private static RoundTxn txn(TxnType type, String gaBetId, String bet, String win) {
        RoundTxn t = new RoundTxn();
        t.setType(type);
        t.setStatus(TxnStatus.SUCCESS);
        t.setGaBetId(gaBetId);
        if (bet != null) t.setBetAmount(new BigDecimal(bet));
        if (win != null) t.setWinAmount(new BigDecimal(win));
        return t;
    }

    @Test
    void roundRollback_sendsOwnPerBetMeta_forEveryBet() {
        BetRollbackContextEnricher enricher = mock(BetRollbackContextEnricher.class);
        GameRoundService gameRoundService = mock(GameRoundService.class);
        GameTransactionService gameTransactionService = mock(GameTransactionService.class);
        ClientResponseValidator clientResponseValidator = mock(ClientResponseValidator.class);
        OperatorApiAdapter operatorApiAdapter = mock(OperatorApiAdapter.class);
        BetHistoryProducer betHistoryProducer = mock(BetHistoryProducer.class);
        RetryQueueService retryQueueService = mock(RetryQueueService.class);
        LegacyCleanupService legacyCleanupService = mock(LegacyCleanupService.class);
        VendorConfigService vendorConfigService = mock(VendorConfigService.class);
        BetTransactionHistoryProducer betTransactionHistoryProducer = mock(BetTransactionHistoryProducer.class);

        BetRollbackProcessor processor = new BetRollbackProcessor(
                enricher, gameRoundService, gameTransactionService, clientResponseValidator,
                operatorApiAdapter, betHistoryProducer, retryQueueService, legacyCleanupService,
                vendorConfigService, betTransactionHistoryProducer);

        // Round with two distinct bets: A (stake 10 / win 20) and B (stake 99 / win 88).
        AgentMeta agentMeta = new AgentMeta();
        agentMeta.setAgentId(AGENT_ID);
        agentMeta.setUsername("user");
        agentMeta.setCurrency("USD");
        agentMeta.setGameCode("game");
        GameRound round = GameRound.of("VendorX", "user", "round-1");
        round.setAgentMeta(agentMeta);
        round.setTransactions(List.of(
                txn(TxnType.BET, "ga-bet-A", "10", null),
                txn(TxnType.RESULT, "ga-bet-A", null, "20"),
                txn(TxnType.BET, "ga-bet-B", "99", null),
                txn(TxnType.RESULT, "ga-bet-B", null, "88")));

        BetRollbackContext ctx = BetRollbackContext.builder().build();
        ctx.setFromVendorRate(BigDecimal.ONE); // operator-POV == vendor amounts at rate 1
        ctx.setToVendorRate(BigDecimal.ONE);

        BetRollbackConfig config = new BetRollbackConfig().allowRollbackForSettledBet(true);

        when(gameRoundService.getOrThrow(any())).thenReturn(round);
        when(gameTransactionService.get(anyString())).thenReturn(Optional.empty());

        ArgumentCaptor<WalletRollbackDto> dtoCaptor = ArgumentCaptor.forClass(WalletRollbackDto.class);
        when(operatorApiAdapter.toApiRequest(dtoCaptor.capture(), eq(AGENT_ID)))
                .thenReturn(mock(OperatorApiRequest.class));
        ApiResult apiResult = mock(ApiResult.class);
        when(operatorApiAdapter.execute(any())).thenReturn(apiResult);
        ClientApiResponse response = mock(ClientApiResponse.class);
        when(apiResult.parseTo(ClientApiResponse.class)).thenReturn(response);
        PlayerBalanceData data = mock(PlayerBalanceData.class);
        when(response.getData()).thenReturn(data);
        when(data.toVendorView(any(), any(), any()))
                .thenReturn(new PlayerBalanceData("user", "USD", BigDecimal.ZERO, 1L));

        processor.processRollbackByRound(ctx, new GameTransaction(), config);

        // One rollback request per bet, each with its own meta.
        verify(operatorApiAdapter, times(2)).toApiRequest(any(WalletRollbackDto.class), eq(AGENT_ID));

        Map<String, String> winByBet = new HashMap<>(); // betAmount -> winAmount
        for (WalletRollbackDto dto : dtoCaptor.getAllValues()) {
            assertNotNull(dto.getMeta(), "each per-bet request must carry meta");
            winByBet.put(
                    dto.getMeta().getBetAmount().stripTrailingZeros().toPlainString(),
                    dto.getMeta().getWinAmount().stripTrailingZeros().toPlainString());
        }
        assertEquals(2, winByBet.size(), "both bets rolled back independently");
        assertEquals("20", winByBet.get("10"));  // bet A
        assertEquals("88", winByBet.get("99"));  // bet B
    }
}
