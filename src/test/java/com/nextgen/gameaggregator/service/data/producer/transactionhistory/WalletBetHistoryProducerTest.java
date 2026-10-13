package com.nextgen.gameaggregator.service.data.producer.transactionhistory;

import com.nextgen.gameaggregator.core.engine.wallet.credit.CreditContext;
import com.nextgen.gameaggregator.core.entity.Agent;
import com.nextgen.gameaggregator.core.entity.GameCategory;
import com.nextgen.gameaggregator.core.entity.Vendor;
import com.nextgen.gameaggregator.core.service.AgentDataService;
import com.nextgen.gameaggregator.core.service.GameCategoryDataService;
import com.nextgen.gameaggregator.core.service.VendorDataService;
import com.nextgen.gameaggregator.entity.couchbase.CreditTxn;
import com.nextgen.gameaggregator.entity.ga.BetHistoryV3;
import com.nextgen.gameaggregator.enums.BetResultType;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.service.KafkaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletBetHistoryProducerTest {

    @Mock
    private KafkaService kafkaService;
    @Mock
    private AgentDataService agentDataService;
    @Mock
    private GameCategoryDataService gameCategoryDataService;
    @Mock
    private VendorDataService vendorDataService;

    @InjectMocks
    private WalletBetHistoryProducer producer;

    private Agent agent;
    private Vendor vendor;
    private GameCategory gameCategory;

    @BeforeEach
    void setUp() {
        agent = new Agent();
        agent.setId(1);
        agent.setHouseId(99);
        agent.setMasterAgentId(11);

        vendor = new Vendor();
        vendor.setId(88);
        vendor.setCode("JDB");
        vendor.setProductId(7);

        gameCategory = new GameCategory();
        gameCategory.setId(3);
        gameCategory.setCode("SLOT");

        when(agentDataService.get(1)).thenReturn(agent);
        when(vendorDataService.get(88)).thenReturn(vendor);
        when(gameCategoryDataService.get(3)).thenReturn(gameCategory);
    }

    private CreditContext.CreditContextBuilder<?, ?> baseBuilder() {
        return CreditContext.builder()
                .traceId("trace-bh")
                .idempotencyKey("transfer-200")
                .vendorTxnId("transfer-200")
                .vendorRoundId("round-1")
                .vendorClassName("jdb")
                .vendorPlayerUsername("bob")
                .vendorPlayerId(5001L)
                .vendorCurrency("PHP")
                .currencyCode("PHP")
                .currencyId(7)
                .vendorId(88)
                .vendorLineId(46)
                .vendorGameId(12)
                .agentId(1)
                .agentPlayerId(900001L)
                .agentPlayerUsername("op-bob")
                .gameCategoryId(3)
                .vendorGameCode("SG12Zodiacs")
                .gameCode("SG12Zodiacs")
                .token("session-token")
                .vendorTimestamp(1700000060000L)
                .fromVendorRate(BigDecimal.ONE)
                .toVendorRate(BigDecimal.ONE);
    }

    private CreditTxn newCredit(GameRoundState state) {
        CreditTxn credit = CreditTxn.of("jdb", "transfer-200", state, 1700000060000L);
        credit.setVendorPlayerUsername("bob");
        credit.setVendorRoundId("round-1");
        return credit;
    }

    @Test
    void settle_winningOutcome_emitsBetHistoryWithSettledStatusAndWinResultType() {
        CreditContext ctx = baseBuilder()
                .isRefund(false)
                .vendorAmount(new BigDecimal("12.00"))
                .vendorBetAmount(new BigDecimal("5.50"))
                .vendorWinAmount(new BigDecimal("12.00"))
                .vendorWinLoss(new BigDecimal("6.50"))
                .vendorEffectiveTurnover(new BigDecimal("5.50"))
                .vendorJackpotAmount(BigDecimal.ZERO)
                .vendorBetTime(1700000000000L)
                .vendorSettleTime(1700000060000L)
                .isRoundEnded(true)
                .build();
        CreditTxn credit = newCredit(GameRoundState.SETTLED);

        producer.publishBetHistoryForCredit(ctx, credit);

        ArgumentCaptor<BetHistoryV3> captor = ArgumentCaptor.forClass(BetHistoryV3.class);
        verify(kafkaService).produceBetHistoryV3(captor.capture());
        BetHistoryV3 h = captor.getValue();

        // Identity
        assertEquals(credit.getGaTxnId(), h.getId(),
                "BetHistory id uses the credit's gaTxnId as a placeholder until Step 3 wires upstream debit lookup");
        assertEquals("transfer-200", h.getExternalTransactionId());
        assertEquals("transfer-200", h.getVendorBetId());
        assertEquals("round-1", h.getRoundId());

        // Vendor
        assertEquals(Integer.valueOf(88), h.getVendorId());
        assertEquals("JDB", h.getVendorCode());
        assertEquals(Integer.valueOf(12), h.getVendorGameId());
        assertEquals(Integer.valueOf(46), h.getVendorLineId());
        assertEquals(Long.valueOf(5001L), h.getVendorPlayerId());
        assertEquals("bob", h.getVendorPlayerUsername());

        // Product (legacy parity: empty/zero)
        assertEquals(Integer.valueOf(7), h.getProductId());
        assertEquals("", h.getProductCode());
        assertEquals(Integer.valueOf(0), h.getProductGameId());

        // Agent
        assertEquals(Integer.valueOf(1), h.getAgentId());
        assertEquals(Long.valueOf(900001L), h.getAgentPlayerId());
        assertEquals("op-bob", h.getAgentPlayerUsername());
        assertEquals(Integer.valueOf(99), h.getHouseId());
        assertEquals(Integer.valueOf(11), h.getMasterAgentId());

        // GA
        assertEquals(Integer.valueOf(3), h.getGameCategoryId());
        assertEquals("SLOT", h.getGameCategoryCode());
        assertEquals("SG12Zodiacs", h.getGameCode());
        assertEquals(Integer.valueOf(7), h.getCurrencyId());
        assertEquals("PHP", h.getCurrencyCode());
        assertEquals("session-token", h.getGameSessionToken());

        // Status / type
        assertEquals(BetStatus.SETTLED.code, h.getStatus());
        assertEquals(BetResultType.WIN.code, h.getResultType(),
                "WIN result type because vendorWinAmount > 0 and jackpot = 0");
        assertEquals(BetType.NORMAL_BET.code, h.getBetType());
        assertEquals(Integer.valueOf(0), h.getIsFreespin());
        assertEquals(Integer.valueOf(0), h.getOperatorStatus());
        assertEquals(Integer.valueOf(0), h.getResettleNum());

        // Amounts (1:1 FX rate)
        assertEquals(0, new BigDecimal("5.50").compareTo(h.getBetAmount()));
        assertEquals(0, new BigDecimal("12.00").compareTo(h.getWinAmount()));
        assertEquals(0, new BigDecimal("6.50").compareTo(h.getWinLoss()));
        assertEquals(0, new BigDecimal("5.50").compareTo(h.getEffectiveTurnover()));
        assertEquals(0, BigDecimal.ZERO.compareTo(h.getJackpotAmount()));

        // Times
        assertEquals(Long.valueOf(1700000000000L), h.getVendorBetTime());
        assertEquals(Long.valueOf(1700000060000L), h.getVendorSettleTime());
        assertNotNull(h.getResultTime());
    }

    @Test
    void settle_losingOutcome_resultTypeIsLose() {
        CreditContext ctx = baseBuilder()
                .isRefund(false)
                .vendorAmount(BigDecimal.ZERO)
                .vendorBetAmount(new BigDecimal("5.00"))
                .vendorWinAmount(BigDecimal.ZERO)
                .vendorWinLoss(new BigDecimal("-5.00"))
                .vendorEffectiveTurnover(new BigDecimal("5.00"))
                .vendorJackpotAmount(BigDecimal.ZERO)
                .build();

        producer.publishBetHistoryForCredit(ctx, newCredit(GameRoundState.SETTLED));

        ArgumentCaptor<BetHistoryV3> captor = ArgumentCaptor.forClass(BetHistoryV3.class);
        verify(kafkaService).produceBetHistoryV3(captor.capture());
        assertEquals(BetResultType.LOSE.code, captor.getValue().getResultType());
        assertEquals(BetStatus.SETTLED.code, captor.getValue().getStatus());
    }

    @Test
    void settle_jackpotOutcome_resultTypeIsJackpot() {
        CreditContext ctx = baseBuilder()
                .isRefund(false)
                .vendorAmount(new BigDecimal("500.00"))
                .vendorBetAmount(new BigDecimal("5.00"))
                .vendorWinAmount(new BigDecimal("500.00"))
                .vendorWinLoss(new BigDecimal("495.00"))
                .vendorEffectiveTurnover(new BigDecimal("5.00"))
                .vendorJackpotAmount(new BigDecimal("450.00"))
                .build();

        producer.publishBetHistoryForCredit(ctx, newCredit(GameRoundState.SETTLED));

        ArgumentCaptor<BetHistoryV3> captor = ArgumentCaptor.forClass(BetHistoryV3.class);
        verify(kafkaService).produceBetHistoryV3(captor.capture());
        assertEquals(BetResultType.JACKPOT.code, captor.getValue().getResultType(),
                "JACKPOT result type takes precedence over WIN when jackpot > 0");
    }

    @Test
    void settle_nullWinLossAndTurnover_fallsBackToComputedValues() {
        // Empirical pattern for debit/credit vendors whose wire doesn't carry winLoss;
        // turnover often falls back to bet.
        CreditContext ctx = baseBuilder()
                .isRefund(false)
                .vendorAmount(new BigDecimal("12.00"))
                .vendorBetAmount(new BigDecimal("5.00"))
                .vendorWinAmount(new BigDecimal("12.00"))
                .vendorWinLoss(null)                    // wire didn't provide
                .vendorEffectiveTurnover(null)          // wire didn't provide
                .vendorJackpotAmount(BigDecimal.ZERO)
                .build();

        producer.publishBetHistoryForCredit(ctx, newCredit(GameRoundState.SETTLED));

        ArgumentCaptor<BetHistoryV3> captor = ArgumentCaptor.forClass(BetHistoryV3.class);
        verify(kafkaService).produceBetHistoryV3(captor.capture());
        BetHistoryV3 h = captor.getValue();

        assertEquals(0, new BigDecimal("7.00").compareTo(h.getWinLoss()),
                "vendorWinLoss=null falls back to vendorWinAmount - vendorBetAmount = 12 - 5 = 7");
        assertEquals(0, new BigDecimal("5.00").compareTo(h.getEffectiveTurnover()),
                "vendorEffectiveTurnover=null falls back to vendorBetAmount = 5");
    }

    @Test
    void settle_nullWinLossWithLosingBet_fallsBackToNegativeWinLoss() {
        CreditContext ctx = baseBuilder()
                .isRefund(false)
                .vendorAmount(BigDecimal.ZERO)
                .vendorBetAmount(new BigDecimal("5.00"))
                .vendorWinAmount(BigDecimal.ZERO)
                .vendorWinLoss(null)                    // wire didn't provide
                .vendorEffectiveTurnover(null)
                .vendorJackpotAmount(BigDecimal.ZERO)
                .build();

        producer.publishBetHistoryForCredit(ctx, newCredit(GameRoundState.SETTLED));

        ArgumentCaptor<BetHistoryV3> captor = ArgumentCaptor.forClass(BetHistoryV3.class);
        verify(kafkaService).produceBetHistoryV3(captor.capture());
        assertEquals(0, new BigDecimal("-5.00").compareTo(captor.getValue().getWinLoss()),
                "losing bet with null winLoss falls back to 0 - 5 = -5 (player lost the full wager)");
    }

    @Test
    void refund_emitsBetHistoryWithRefundedStatusAndNegativeWinLoss() {
        CreditContext ctx = baseBuilder()
                .vendorTxnId("transfer-300")
                .isRefund(true)
                .vendorAmount(new BigDecimal("5.50"))
                .vendorBetAmount(new BigDecimal("5.50"))
                .referencedVendorDebitTxnIds(List.of("transfer-100"))
                .build();
        CreditTxn credit = newCredit(GameRoundState.REFUNDED);
        credit.setVendorTxnId("transfer-300");

        producer.publishBetHistoryForCredit(ctx, credit);

        ArgumentCaptor<BetHistoryV3> captor = ArgumentCaptor.forClass(BetHistoryV3.class);
        verify(kafkaService).produceBetHistoryV3(captor.capture());
        BetHistoryV3 h = captor.getValue();

        assertEquals(BetStatus.REFUNDED.code, h.getStatus(),
                "refund emits REFUNDED status");
        assertEquals(BetResultType.BET.code, h.getResultType(),
                "refund uses BET result type (no win/loss/jackpot calculation applies)");

        // Mirrors legacy WalletBetCreditProcessor: betAmount = refund value, winAmount = 0,
        // winLoss = winAmount - betAmount = -refundBet (negative — represents money returned).
        assertEquals(0, new BigDecimal("5.50").compareTo(h.getBetAmount()),
                "betAmount = original wager being refunded");
        assertEquals(0, BigDecimal.ZERO.compareTo(h.getWinAmount()));
        assertEquals(0, new BigDecimal("-5.50").compareTo(h.getWinLoss()),
                "winLoss = 0 - 5.50 = -5.50; downstream BI summing winLoss sees the refund as negative PNL");
        assertEquals(0, BigDecimal.ZERO.compareTo(h.getEffectiveTurnover()));
        assertEquals(0, BigDecimal.ZERO.compareTo(h.getJackpotAmount()));
    }

    @Test
    void refund_betAmountFallsBackToVendorAmountWhenBetAmountIsNull() {
        CreditContext ctx = baseBuilder()
                .vendorTxnId("transfer-300")
                .isRefund(true)
                .vendorAmount(new BigDecimal("7.25"))
                .vendorBetAmount(null)
                .referencedVendorDebitTxnIds(List.of("transfer-100"))
                .build();

        producer.publishBetHistoryForCredit(ctx, newCredit(GameRoundState.REFUNDED));

        ArgumentCaptor<BetHistoryV3> captor = ArgumentCaptor.forClass(BetHistoryV3.class);
        verify(kafkaService).produceBetHistoryV3(captor.capture());
        BetHistoryV3 h = captor.getValue();
        assertEquals(0, new BigDecimal("7.25").compareTo(h.getBetAmount()),
                "when vendorBetAmount is null, betAmount falls back to refund value (vendorAmount)");
        assertEquals(0, new BigDecimal("-7.25").compareTo(h.getWinLoss()),
                "winLoss tracks the fallback betAmount: 0 - 7.25 = -7.25");
    }

}
