package com.nextgen.gameaggregator.vendor.casinogate;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContextMapper;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceVendorResponseMapper;
import com.nextgen.gameaggregator.core.engine.wallet.balance.WalletBalanceService;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.WalletRollbackServiceWrapper;
import com.nextgen.gameaggregator.core.exception.BetNotFoundException;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.enums.TxnType;
import com.nextgen.gameaggregator.vendor.casinogate.api.balance.BalanceController;
import com.nextgen.gameaggregator.vendor.casinogate.api.balance.BalanceRequest;
import com.nextgen.gameaggregator.vendor.casinogate.api.balance.BalanceRequestMapper;
import com.nextgen.gameaggregator.vendor.casinogate.api.balance.BalanceResponseMapper;
import com.nextgen.gameaggregator.vendor.casinogate.api.bet.BetController;
import com.nextgen.gameaggregator.vendor.casinogate.api.bet.BetRequest;
import com.nextgen.gameaggregator.vendor.casinogate.api.bet.BetRequestMapper;
import com.nextgen.gameaggregator.vendor.casinogate.api.bet.BetResponseMapper;
import com.nextgen.gameaggregator.vendor.casinogate.api.refund.RefundController;
import com.nextgen.gameaggregator.vendor.casinogate.api.refund.RefundRequest;
import com.nextgen.gameaggregator.vendor.casinogate.api.refund.RefundRequestMapper;
import com.nextgen.gameaggregator.vendor.casinogate.api.refund.RefundResponseMapper;
import com.nextgen.gameaggregator.vendor.casinogate.api.win.WinController;
import com.nextgen.gameaggregator.vendor.casinogate.api.win.WinRequest;
import com.nextgen.gameaggregator.vendor.casinogate.api.win.WinRequestMapper;
import com.nextgen.gameaggregator.vendor.casinogate.api.win.WinResponseMapper;
import com.nextgen.gameaggregator.vendor.casinogate.response.CommonResponse;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CasinoGateEndpointTest {
    private WalletBalanceService balanceService;
    private WalletBetService betService;
    private WalletBetResultServiceWrapper winService;
    private WalletRollbackServiceWrapper refundService;
    private BalanceController balanceController;
    private BetController betController;
    private WinController winController;
    private RefundController refundController;
    private Validator validator;

    @BeforeEach
    void setUp() {
        balanceService = mock(WalletBalanceService.class);
        betService = mock(WalletBetService.class);
        winService = mock(WalletBetResultServiceWrapper.class);
        refundService = mock(WalletRollbackServiceWrapper.class);

        balanceController = new TestBalanceController(
                new BalanceRequestMapper(), new BalanceResponseMapper(), balanceService);
        betController = new BetController(
                new BetRequestMapper(), new BetResponseMapper(), betService);
        winController = new WinController(
                new WinRequestMapper(), new WinResponseMapper(), winService);
        refundController = new RefundController(
                new RefundRequestMapper(), new RefundResponseMapper(), refundService);
        validator = Validation.buildDefaultValidatorFactory().getValidator();

        LogContext logContext = new LogContext();
        logContext.setVendorClassName("casinoGate");
        LogContextHolder.set(logContext);
    }

    @AfterEach
    void tearDown() {
        LogContextHolder.clear();
    }

    @Test
    void walletReturnsBalanceInMinorUnits() {
        when(balanceService.process(any(BalanceContext.class))).thenReturn(balance());

        BalanceRequest request = new BalanceRequest();
        request.setToken("session-token");

        CommonResponse response = balanceController.balance(request).getBody();

        assertThat(response).isNotNull();
        assertThat(response.getBalance()).isEqualTo(BigInteger.valueOf(12345));
        assertThat(response.getCurrency()).isEqualTo("EUR");
        assertThat(response.getDenomination()).isEqualTo(100);
        assertThat(response.getBuffer()).isEmpty();

        ArgumentCaptor<BalanceContext> context = ArgumentCaptor.forClass(BalanceContext.class);
        verify(balanceService).process(context.capture());
        assertThat(context.getValue().getToken()).isEqualTo("session-token");
    }

    @Test
    void placeBetConvertsAmountToMajorUnits() {
        when(betService.initialise(any(BetContext.class))).thenReturn(betService);
        when(betService.configure(any())).thenReturn(betService);
        when(betService.process()).thenReturn(balance());

        BetRequest request = new BetRequest();
        request.setAmount(250L);
        request.setRoundId("round-1");
        request.setToken("session-token");
        request.setTransactionId("bet-1");

        CommonResponse response = betController.bet(request).getBody();

        assertThat(response).isNotNull();
        assertThat(response.getBalance()).isEqualTo(BigInteger.valueOf(12345));

        ArgumentCaptor<BetContext> context = ArgumentCaptor.forClass(BetContext.class);
        verify(betService).initialise(context.capture());
        assertThat(context.getValue().getBetAmount()).isEqualByComparingTo("2.50");
        assertThat(context.getValue().getRoundId()).isEqualTo("round-1");
        assertThat(context.getValue().getIdempotencyKey()).isEqualTo("bet-1");
    }

    @Test
    void winMapsEndRoundAndAmount() {
        when(winService.initialise(any(BetResultContext.class))).thenReturn(winService);
        when(winService.configure(any())).thenReturn(winService);
        when(winService.process()).thenReturn(balance());

        WinRequest request = validWinRequest();

        CommonResponse response = winController.win(request).getBody();

        assertThat(response).isNotNull();
        assertThat(response.getBalance()).isEqualTo(BigInteger.valueOf(12345));

        ArgumentCaptor<BetResultContext> context = ArgumentCaptor.forClass(BetResultContext.class);
        verify(winService).initialise(context.capture());
        assertThat(context.getValue().getWinAmount()).isEqualByComparingTo("5.00");
        assertThat(context.getValue().isRoundEnded()).isTrue();
        assertThat(context.getValue().getIdempotencyKey()).isEqualTo("win-1");
        assertThat(context.getValue().getVendorBetId()).isEqualTo("bet-1");
    }

    @Test
    void winIdempotencyKeyBlocksReusedTransactionIdAcrossDifferentBets() {
        WinRequest roundASettle = validWinRequest();
        roundASettle.setRoundId("roundA");
        roundASettle.setBetTransactionId("bet1");
        roundASettle.setTransactionId("transactionIdA");

        WinRequest roundBSettle = validWinRequest();
        roundBSettle.setRoundId("roundB");
        roundBSettle.setBetTransactionId("bet2");
        roundBSettle.setTransactionId("transactionIdA");

        BetResultContext roundAContext = new WinRequestMapper().toInternal(roundASettle);
        BetResultContext roundBContext = new WinRequestMapper().toInternal(roundBSettle);

        assertThat(roundASettle.getTransactionId()).isEqualTo(roundBSettle.getTransactionId());
        assertThat(roundAContext.getIdempotencyKey()).isEqualTo("transactionIdA");
        assertThat(roundBContext.getIdempotencyKey()).isEqualTo("transactionIdA");
        assertThat(roundAContext.getIdempotencyKey()).isEqualTo(roundBContext.getIdempotencyKey());
    }

    @Test
    void winDefaultsMissingEndRoundToTrue() {
        when(winService.initialise(any(BetResultContext.class))).thenReturn(winService);
        when(winService.configure(any())).thenReturn(winService);
        when(winService.process()).thenReturn(balance());

        WinRequest request = validWinRequest();
        request.setEndRound(null);

        assertThat(validator.validate(request)).isEmpty();

        winController.win(request);

        ArgumentCaptor<BetResultContext> context = ArgumentCaptor.forClass(BetResultContext.class);
        verify(winService).initialise(context.capture());
        assertThat(context.getValue().isRoundEnded()).isTrue();
    }

    @Test
    void winPreservesExplicitFalseEndRound() {
        when(winService.initialise(any(BetResultContext.class))).thenReturn(winService);
        when(winService.configure(any())).thenReturn(winService);
        when(winService.process()).thenReturn(balance());

        WinRequest request = validWinRequest();
        request.setEndRound(false);

        winController.win(request);

        ArgumentCaptor<BetResultContext> context = ArgumentCaptor.forClass(BetResultContext.class);
        verify(winService).initialise(context.capture());
        assertThat(context.getValue().isRoundEnded()).isFalse();
    }

    @Test
    void refundMapsRefundTransactionIdAndOriginalBetTransactionId() {
        when(refundService.initialise(any(BetRollbackContext.class))).thenReturn(refundService);
        when(refundService.configure(any())).thenReturn(refundService);
        when(refundService.process()).thenReturn(balance());

        RefundRequest request = new RefundRequest();
        request.setAmount(250L);
        request.setRefundTransactionId("refund-1");
        request.setRoundId("round-1");
        request.setToken("session-token");
        request.setTransactionId("bet-1");

        CommonResponse response = refundController.refund(request).getBody();

        assertThat(response).isNotNull();
        assertThat(response.getBalance()).isEqualTo(BigInteger.valueOf(12345));

        ArgumentCaptor<BetRollbackContext> context = ArgumentCaptor.forClass(BetRollbackContext.class);
        verify(refundService).initialise(context.capture());
        assertThat(context.getValue().getIdempotencyKey()).isEqualTo("refund-1");
        assertThat(context.getValue().getVendorBetId()).isEqualTo("bet-1");
        assertThat(context.getValue().getRoundId()).isEqualTo("round-1");
    }

    @Test
    void settleByBetRejectsBetTransactionFromAnotherRound() {
        GameRound roundA = roundWithTransactions("roundA",
                successfulBet("roundA", "bet1", GameRoundState.UNSETTLED));
        GameTransaction resultTxn = resultTxn("roundA", "bet2");

        assertThatThrownBy(() -> invokeSettleSpecificBet(roundA, resultTxn))
                .isInstanceOf(BetNotFoundException.class)
                .hasMessageContaining("roundA")
                .hasMessageContaining("bet2");
    }

    @Test
    void settleByBetRejectsAlreadySettledBetInSameRound() {
        GameRound roundA = roundWithTransactions("roundA",
                successfulBet("roundA", "bet2", GameRoundState.SETTLED));
        GameTransaction resultTxn = resultTxn("roundA", "bet2");

        assertThatThrownBy(() -> invokeSettleSpecificBet(roundA, resultTxn))
                .isInstanceOf(BetNotFoundException.class)
                .hasMessageContaining("roundA")
                .hasMessageContaining("bet2");
    }

    private PlayerBalanceData balance() {
        return new PlayerBalanceData("player-1", "EUR", new BigDecimal("123.45"), 1L);
    }

    private GameRound roundWithTransactions(String roundId, RoundTxn... txns) {
        GameRound round = GameRound.of("casinogate", "player-1", roundId);
        round.setTransactions(List.of(txns));
        return round;
    }

    private RoundTxn successfulBet(String roundId, String betTransactionId, GameRoundState state) {
        GameTransaction betTxn = GameTransaction.of(TxnType.BET, "casinogate", betTransactionId, 1L);
        betTxn.setRoundId(roundId);
        betTxn.setUsername("player-1");
        betTxn.setVendorBetId(betTransactionId);
        betTxn.setStatus(TxnStatus.SUCCESS);
        betTxn.setState(state);
        return RoundTxn.of(betTxn);
    }

    private GameTransaction resultTxn(String roundId, String betTransactionId) {
        GameTransaction resultTxn = GameTransaction.of(TxnType.RESULT, "casinogate", roundId + "_" + betTransactionId, 2L);
        resultTxn.setRoundId(roundId);
        resultTxn.setUsername("player-1");
        resultTxn.setVendorBetId(betTransactionId);
        resultTxn.setState(GameRoundState.SETTLED);
        return resultTxn;
    }

    private RoundTxn invokeSettleSpecificBet(GameRound round, GameTransaction resultTxn) {
        try {
            Class<?> processorClass = Class.forName("com.nextgen.gameaggregator.core.engine.wallet.result.BetResultProcessor");
            Constructor<?> constructor = processorClass.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            Object processor = constructor.newInstance(new Object[constructor.getParameterCount()]);

            Method method = processorClass.getDeclaredMethod("settleSpecificBet", GameRound.class, GameTransaction.class);
            method.setAccessible(true);
            return (RoundTxn) method.invoke(processor, round, resultTxn);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new AssertionError(cause);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    private WinRequest validWinRequest() {
        WinRequest request = new WinRequest();
        request.setAmount(500L);
        request.setBetTransactionId("bet-1");
        request.setEndRound(true);
        request.setRoundId("round-1");
        request.setToken("session-token");
        request.setTransactionId("win-1");
        return request;
    }

    public static final class TestBalanceController extends BalanceController {
        private TestBalanceController(BalanceContextMapper<BalanceRequest> requestMapper,
                                      BalanceVendorResponseMapper<CommonResponse> responseMapper,
                                      WalletBalanceService walletService) {
            super(requestMapper, responseMapper, walletService);
        }
    }
}
