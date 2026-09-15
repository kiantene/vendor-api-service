package com.nextgen.gameaggregator.vendor.bglive.service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.service.BetActionLogService;
import com.nextgen.gameaggregator.service.BetNotFoundLogService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.SettledBetService;
import com.nextgen.gameaggregator.service.UnsettledBetCachingService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.bglive.constant.QueryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@code VendorService.settledBetIdempotentCheck}.
 * ONEAPI-293: don't trust winLoss when settled_bet is REFUNDED/CANCELLED, or the
 * last operator push explicitly failed (operatorStatus set but != SC_OK). A null
 * operatorStatus (unset on the EndRoundProcessor multi-bet-per-round path) must not
 * be treated as a failed push.
 */
@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

    private static final String EXTERNAL_ID = "17739292332";
    private static final Long VENDOR_PLAYER_ID = 35001L;

    @Mock
    private GameSessionService gameSessionService;
    @Mock
    private UnsettledBetCachingService unsettledBetCachingService;
    @Mock
    private SettledBetService settledBetService;
    @Mock
    private WalletService walletService;
    @Mock
    private WalletRequestService walletRequestService;
    @Mock
    private BetActionLogService betActionLogService;
    @Mock
    private BetNotFoundLogService betNotFoundLogService;
    @Mock
    private VendorLineService vendorLineService;

    @InjectMocks
    private VendorService vendorService;

    private GameSession gameSession;

    @BeforeEach
    void setUp() {
        gameSession = new GameSession();
        gameSession.setVendorPlayerId(VENDOR_PLAYER_ID);
    }

    private SettledBet buildSettledBet(Integer status, Integer operatorStatus, BigDecimal winLoss) {
        SettledBet settledBet = new SettledBet();
        settledBet.setStatus(status);
        settledBet.setOperatorStatus(operatorStatus);
        settledBet.setWinLoss(winLoss);
        return settledBet;
    }

    @Test
    void refundedSettledBet_returnsNoBet_evenThoughWinLossIsNegative() throws BetNotFoundException {
        // REFUNDED settled_bet with a real winLoss - must not settle.
        SettledBet settledBet = buildSettledBet(BetStatus.REFUNDED.code, ResponseCodes.Status.SC_TRANSACTION_NOT_EXISTS.code, new BigDecimal("-20"));
        when(settledBetService.getByVendorPlayerIdAndExternalTransactionId(VENDOR_PLAYER_ID, EXTERNAL_ID)).thenReturn(settledBet);

        Integer status = vendorService.settledBetIdempotentCheck(gameSession, EXTERNAL_ID);

        assertThat(status).isEqualTo(QueryStatus.NO_BET);
    }

    @Test
    void cancelledSettledBet_returnsNoBet_evenWithSuccessfulOperatorStatus() throws BetNotFoundException {
        // CANCELLED settled_bet, even with a successful operator push.
        SettledBet settledBet = buildSettledBet(BetStatus.CANCELLED.code, ResponseCodes.Status.SC_OK.code, new BigDecimal("20"));
        when(settledBetService.getByVendorPlayerIdAndExternalTransactionId(VENDOR_PLAYER_ID, EXTERNAL_ID)).thenReturn(settledBet);

        Integer status = vendorService.settledBetIdempotentCheck(gameSession, EXTERNAL_ID);

        assertThat(status).isEqualTo(QueryStatus.NO_BET);
    }

    @Test
    void settledStatus_withFailedOperatorPush_returnsNoBet() throws BetNotFoundException {
        // status still SETTLED, but the operator push failed.
        SettledBet settledBet = buildSettledBet(BetStatus.SETTLED.code, ResponseCodes.Status.SC_TRANSACTION_NOT_EXISTS.code, new BigDecimal("-20"));
        when(settledBetService.getByVendorPlayerIdAndExternalTransactionId(VENDOR_PLAYER_ID, EXTERNAL_ID)).thenReturn(settledBet);

        Integer status = vendorService.settledBetIdempotentCheck(gameSession, EXTERNAL_ID);

        assertThat(status).isEqualTo(QueryStatus.NO_BET);
    }

    @Test
    void settledStatus_withSuccessfulOperatorPush_andPositiveWinLoss_returnsSettleWin() throws BetNotFoundException {
        SettledBet settledBet = buildSettledBet(BetStatus.SETTLED.code, ResponseCodes.Status.SC_OK.code, new BigDecimal("20"));
        when(settledBetService.getByVendorPlayerIdAndExternalTransactionId(VENDOR_PLAYER_ID, EXTERNAL_ID)).thenReturn(settledBet);

        Integer status = vendorService.settledBetIdempotentCheck(gameSession, EXTERNAL_ID);

        assertThat(status).isEqualTo(QueryStatus.SETTLE_WIN);
    }

    @Test
    void settledStatus_withSuccessfulOperatorPush_andNegativeWinLoss_returnsSettleLose() throws BetNotFoundException {
        SettledBet settledBet = buildSettledBet(BetStatus.SETTLED.code, ResponseCodes.Status.SC_OK.code, new BigDecimal("-20"));
        when(settledBetService.getByVendorPlayerIdAndExternalTransactionId(VENDOR_PLAYER_ID, EXTERNAL_ID)).thenReturn(settledBet);

        Integer status = vendorService.settledBetIdempotentCheck(gameSession, EXTERNAL_ID);

        assertThat(status).isEqualTo(QueryStatus.SETTLE_LOSE);
    }

    @Test
    void settledStatus_withSuccessfulOperatorPush_andZeroWinLoss_returnsSettleTie() throws BetNotFoundException {
        SettledBet settledBet = buildSettledBet(BetStatus.SETTLED.code, ResponseCodes.Status.SC_OK.code, BigDecimal.ZERO);
        when(settledBetService.getByVendorPlayerIdAndExternalTransactionId(VENDOR_PLAYER_ID, EXTERNAL_ID)).thenReturn(settledBet);

        Integer status = vendorService.settledBetIdempotentCheck(gameSession, EXTERNAL_ID);

        assertThat(status).isEqualTo(QueryStatus.SETTLE_TIE);
    }

    @Test
    void settledStatus_withNullOperatorStatus_andNegativeWinLoss_returnsSettleLose() throws BetNotFoundException {
        // EndRoundProcessor's multi-bet-per-round path never sets operatorStatus - null
        // here is a legitimately settled bet, not a failed push, and must not become NO_BET.
        SettledBet settledBet = buildSettledBet(BetStatus.SETTLED.code, null, new BigDecimal("-20"));
        when(settledBetService.getByVendorPlayerIdAndExternalTransactionId(VENDOR_PLAYER_ID, EXTERNAL_ID)).thenReturn(settledBet);

        Integer status = vendorService.settledBetIdempotentCheck(gameSession, EXTERNAL_ID);

        assertThat(status).isEqualTo(QueryStatus.SETTLE_LOSE);
    }
}
