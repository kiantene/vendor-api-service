package com.nextgen.gameaggregator.vendor.cq9.api.refund;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestServiceImpl;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.Vendor;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.entity.ga.WalletTransaction;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletServiceImpl;
import com.nextgen.gameaggregator.service.GameServiceImpl;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.UnsettledBetService;
import com.nextgen.gameaggregator.service.VendorCurrencyService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.VendorPlayerService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.service.WalletTransactionServiceImpl;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cq9.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cq9.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.cq9.service.VendorService;
import com.nextgen.gameaggregator.vendor.cq9.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.cq9.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GA-14520 — CQ9 refund for a bet GA never accepted.
 *
 * Fix: when a refund's bet exists in neither table, ack success (code 0) instead of 1014 so CQ9 stops resending.
 * The success body carries the player's live balance (or zero if no session) and the VENDOR-FACING currency
 * (resolved at account level via VendorCurrency — the refund has no game code); the no-op is a warning log,
 * NOT a write to the bet_not_found_logs dedup collection.
 */
class RefundActionTest {

    private GameSessionService gameSessionService;
    private HttpService httpService;
    private VendorLineService vendorLineService;
    private WalletService walletService;
    private VendorService vendorService;
    private UnsettledBetService unsettledBetService;
    private OperatorWalletServiceImpl operatorWalletService;
    private WalletRequestServiceImpl walletRequestService;
    private WalletTransactionServiceImpl walletTransactionService;
    private RequestIdempotentLogService requestIdempotentLogService;
    private VendorPlayerService vendorPlayerService;
    private GameServiceImpl gameService;
    private VendorCurrencyService vendorCurrencyService;

    private RefundAction refundAction;

    private static final Integer VENDOR_ID = 3;
    private static final Integer CURRENCY_ID = 7;
    private static final String VENDOR_CURRENCY = "BRL";   // vendor-facing LATAM currency (not a hardcoded default)
    private static final String MTCODE = "pro-rollout-AT05m7688558m1m1:cq9";
    private static final String ACCOUNT = "3j2wt8x21ufh";
    private static final String REQUEST_BODY = "account=" + ACCOUNT + "&mtcode=" + MTCODE;

    @BeforeEach
    void setUp() throws Exception {
        gameSessionService = mock(GameSessionService.class);
        httpService = mock(HttpService.class);
        vendorLineService = mock(VendorLineService.class);
        walletService = mock(WalletService.class);
        vendorService = mock(VendorService.class);
        unsettledBetService = mock(UnsettledBetService.class);
        operatorWalletService = mock(OperatorWalletServiceImpl.class);
        walletRequestService = mock(WalletRequestServiceImpl.class);
        walletTransactionService = mock(WalletTransactionServiceImpl.class);
        requestIdempotentLogService = mock(RequestIdempotentLogService.class);
        vendorPlayerService = mock(VendorPlayerService.class);
        gameService = mock(GameServiceImpl.class);
        vendorCurrencyService = mock(VendorCurrencyService.class);

        refundAction = new RefundAction(gameSessionService, httpService, vendorLineService, walletService,
                vendorService, unsettledBetService, operatorWalletService, walletRequestService,
                walletTransactionService, requestIdempotentLogService, vendorPlayerService, gameService,
                vendorCurrencyService);

        HttpRequestLog log = new HttpRequestLog();
        log.setId("trace-GA14520");
        log.setRequestBody(REQUEST_BODY);
        when(httpService.start(any(HttpServletRequest.class))).thenReturn(log);

        Vendor cq9 = mock(Vendor.class);
        when(cq9.getId()).thenReturn(VENDOR_ID);
        when(vendorService.findVendorByCode(Credentials.VENDOR_CODE)).thenReturn(cq9);

        when(requestIdempotentLogService.checkExists(any(RollbackData.class), anyString())).thenReturn(null);

        when(unsettledBetService.getByVendorIdAndExternalTransactionId(eq(VENDOR_ID), anyString()))
                .thenThrow(new BetNotFoundException("no unsettled bet for " + MTCODE));
    }

    private void stubVendorFacingCurrency() throws Exception {
        VendorPlayer player = mock(VendorPlayer.class);
        when(player.getId()).thenReturn(42L);
        when(player.getCurrencyId()).thenReturn(CURRENCY_ID);
        when(vendorPlayerService.getVendorPlayerByUsername(ACCOUNT)).thenReturn(player);
        when(vendorCurrencyService.getVendorCurrencyCode(VENDOR_ID, CURRENCY_ID)).thenReturn(VENDOR_CURRENCY);
    }

    private ResponseVo<CommonVo> runRefund() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("wtoken")).thenReturn("wtoken-value");
        try (MockedStatic<HttpService> http = mockStatic(HttpService.class);
             MockedStatic<ValidationUtils> validation = mockStatic(ValidationUtils.class)) {
            RefundDto dto = new RefundDto();
            dto.setMtcode(MTCODE);
            dto.setAccount(ACCOUNT);
            http.when(() -> HttpService.convertQueryStringToDtoUrlDecode(anyString(), eq(RefundDto.class)))
                    .thenReturn(dto);
            return refundAction.refund(request);
        }
    }

    /** No bet in either table, no live session: ack success with ZERO balance + the vendor-facing currency. */
    @Test
    void missingBetRefund_noSession_acksSuccessZeroBalance() throws Exception {
        when(walletTransactionService.getByVendorIdAndExternalTransactionId(any(), any())).thenReturn(null);
        stubVendorFacingCurrency();
        when(gameService.getGameSessionByUsername(ACCOUNT)).thenThrow(new AuthenticationException());

        ResponseVo<CommonVo> response = runRefund();

        assertEquals(ResponseCodes.SUCCESS, response.getStatus().getCode(),
                "un-actionable refund must be acked as success so CQ9 stops resending");
        assertEquals(BigDecimal.ZERO.setScale(4), response.getData().getBalance(), "no debit occurred -> zero");
        assertEquals(VENDOR_CURRENCY, response.getData().getCurrency(), "vendor-facing currency, never GA-internal/hardcoded");
    }

    /** No bet, but a live session: ack success with the player's live operator balance + vendor-facing currency. */
    @Test
    void missingBetRefund_withLiveSession_acksLiveBalance() throws Exception {
        when(walletTransactionService.getByVendorIdAndExternalTransactionId(any(), any())).thenReturn(null);
        stubVendorFacingCurrency();
        GameSession session = mock(GameSession.class);
        when(gameService.getGameSessionByUsername(ACCOUNT)).thenReturn(session);
        when(walletService.getBalance(anyString(), any(GameSession.class), any(HttpRequestLog.class)))
                .thenReturn(new BigDecimal("5.0000"));

        ResponseVo<CommonVo> response = runRefund();

        assertEquals(ResponseCodes.SUCCESS, response.getStatus().getCode());
        assertEquals(new BigDecimal("5.0000"), response.getData().getBalance(), "live operator balance");
        assertEquals(VENDOR_CURRENCY, response.getData().getCurrency());
    }

    /** Unknown player on the no-bet lookup must return PLAYER_NOT_FOUND (1006), not 1100 (which CQ9 resends). */
    @Test
    void missingBetRefund_unknownPlayer_returnsPlayerNotFound_notServerError() throws Exception {
        when(walletTransactionService.getByVendorIdAndExternalTransactionId(any(), any())).thenReturn(null);
        when(vendorPlayerService.getVendorPlayerByUsername(ACCOUNT)).thenThrow(new InvalidPlayerException());

        ResponseVo<CommonVo> response = runRefund();

        assertEquals(ResponseCodes.PLAYER_NOT_FOUND, response.getStatus().getCode(),
                "unknown player -> 1006, not a 1100 that CQ9 would resend");
    }

    /** Guard: when a WalletTransaction exists, the existing betCredit path runs unchanged — the fix must not
     *  broaden into it, and the no-bet ack services must not be touched. */
    @Test
    void walletTransactionPresent_takesExistingCreditPath_notNoBetAck() throws Exception {
        WalletTransaction wt = mock(WalletTransaction.class);
        when(walletTransactionService.getByVendorIdAndExternalTransactionId(any(), any())).thenReturn(wt);
        WalletRequest credited = mock(WalletRequest.class);
        when(credited.getBalanceAfter()).thenReturn(new BigDecimal("12.3400"));
        when(credited.getCurrencyCode()).thenReturn(VENDOR_CURRENCY);
        when(operatorWalletService.betCredit(any(WalletRequest.class))).thenReturn(credited);

        ResponseVo<CommonVo> response = runRefund();

        assertEquals(ResponseCodes.SUCCESS, response.getStatus().getCode(), "existing credit path returns success");
        verify(operatorWalletService).betCredit(any(WalletRequest.class));
        verify(vendorPlayerService, never()).getVendorPlayerByUsername(anyString());
        verify(vendorCurrencyService, never()).getVendorCurrencyCode(any(), any());
    }
}
