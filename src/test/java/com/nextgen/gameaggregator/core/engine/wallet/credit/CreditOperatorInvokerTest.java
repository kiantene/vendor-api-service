package com.nextgen.gameaggregator.core.engine.wallet.credit;

import com.couchbase.client.core.error.CasMismatchException;
import com.nextgen.gameaggregator.core.common.OperatorRequestObject;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.idempotency.WalletIdempotencyResolver;
import com.nextgen.gameaggregator.core.engine.operator.OperatorApiService;
import com.nextgen.gameaggregator.core.engine.operator.OperatorCallContext;
import com.nextgen.gameaggregator.core.engine.operator.wallet.credit.CreditOperatorWalletAdapter;
import com.nextgen.gameaggregator.core.engine.operator.wallet.credit.OperatorCreditRequest;
import com.nextgen.gameaggregator.core.engine.operator.wallet.credit.OperatorCreditRequestMapper;
import com.nextgen.gameaggregator.core.vendor.config.VendorConfigService;
import com.nextgen.gameaggregator.entity.couchbase.CreditTxn;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.service.data.CreditTxnDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditOperatorInvokerTest {

    @Mock private OperatorApiService operatorApiService;
    @Mock private OperatorCreditRequestMapper operatorCreditRequestMapper;
    @Mock private CreditOperatorWalletAdapter creditOperatorWalletAdapter;
    @Mock private VendorConfigService vendorConfigService;
    @Mock private CreditTxnDataService creditTxnDataService;

    @InjectMocks private CreditOperatorInvoker invoker;

    private CreditContext settleContext() {
        return CreditContext.builder()
                .vendorTxnId("transfer-200")
                .vendorClassName("jdb")
                .vendorPlayerUsername("bob")
                .vendorRoundId("round-1")
                .vendorCurrency("PHP")
                .currencyCode("PHP")
                .vendorId(88)
                .agentId(1)
                .agentPlayerUsername("op-bob")
                .token("session-token")
                .fromVendorRate(BigDecimal.ONE)
                .toVendorRate(BigDecimal.ONE)
                .isRefund(false)
                .build();
    }

    private CreditTxn freshTxn() {
        return CreditTxn.of("jdb", "transfer-200", GameRoundState.SETTLED, 1L);
    }

    @Test
    void happy_marksSentThenSuccessAndReturnsVendorView() {
        when(vendorConfigService.getTimeoutInMillis("jdb")).thenReturn(4000);
        when(operatorCreditRequestMapper.toOperatorRequest(
                ArgumentMatchers.any(CreditContext.class),
                ArgumentMatchers.any(CreditTxn.class))).thenReturn(new OperatorCreditRequest());
        when(operatorApiService.execute(
                ArgumentMatchers.eq(creditOperatorWalletAdapter),
                ArgumentMatchers.<OperatorCallContext<OperatorRequestObject>>any(),
                ArgumentMatchers.any()))
                .thenReturn(new PlayerBalanceData("op-bob", "PHP", new BigDecimal("106.50"), 0L));

        CreditTxn txn = freshTxn();
        PlayerBalanceData result = invoker.invoke(settleContext(), txn, WalletIdempotencyResolver.NO_CAS);

        assertEquals(0, new BigDecimal("106.50").compareTo(result.getBalance()));
        verify(creditTxnDataService).markSent(txn, WalletIdempotencyResolver.NO_CAS);
        verify(creditTxnDataService).markSuccess(
                ArgumentMatchers.eq(txn),
                argThat(b -> b != null && b.compareTo(new BigDecimal("106.50")) == 0));
        verify(creditTxnDataService, never()).markError(ArgumentMatchers.any(), ArgumentMatchers.anyString());
    }

    @Test
    void dispatchesToCreditEndpointWithTargetBuiltFromContext() {
        when(vendorConfigService.getTimeoutInMillis("jdb")).thenReturn(4000);
        when(operatorCreditRequestMapper.toOperatorRequest(
                ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(new OperatorCreditRequest());
        when(operatorApiService.execute(
                ArgumentMatchers.any(CreditOperatorWalletAdapter.class),
                ArgumentMatchers.<OperatorCallContext<OperatorRequestObject>>any(),
                ArgumentMatchers.any()))
                .thenReturn(new PlayerBalanceData("op-bob", "PHP", new BigDecimal("1"), 0L));

        invoker.invoke(settleContext(), freshTxn(), WalletIdempotencyResolver.NO_CAS);

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<OperatorCallContext<OperatorRequestObject>> cap =
                (ArgumentCaptor) ArgumentCaptor.forClass(OperatorCallContext.class);
        verify(operatorApiService).execute(
                ArgumentMatchers.eq(creditOperatorWalletAdapter), cap.capture(), ArgumentMatchers.any());
        OperatorCallContext<OperatorRequestObject> ctx = cap.getValue();
        assertEquals(EndPoints.WALLET_BET_CREDIT, ctx.endpoint());
        assertEquals(Integer.valueOf(1), ctx.target().agentId());
        assertEquals("op-bob", ctx.target().agentPlayerUsername());
        assertEquals("PHP", ctx.target().currency());
        assertEquals("session-token", ctx.target().sessionToken());
    }

    @Test
    void operatorThrows_marksErrorAndRethrows() {
        when(vendorConfigService.getTimeoutInMillis("jdb")).thenReturn(4000);
        when(operatorCreditRequestMapper.toOperatorRequest(
                ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(new OperatorCreditRequest());
        when(operatorApiService.execute(
                ArgumentMatchers.any(CreditOperatorWalletAdapter.class),
                ArgumentMatchers.<OperatorCallContext<OperatorRequestObject>>any(),
                ArgumentMatchers.any()))
                .thenThrow(new RuntimeException("boom"));

        CreditTxn txn = freshTxn();
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> invoker.invoke(settleContext(), txn, WalletIdempotencyResolver.NO_CAS));

        assertEquals("boom", ex.getMessage());
        verify(creditTxnDataService).markSent(txn, WalletIdempotencyResolver.NO_CAS);
        verify(creditTxnDataService).markError(
                ArgumentMatchers.eq(txn), ArgumentMatchers.eq("RuntimeException"));
        verify(creditTxnDataService, never()).markSuccess(
                ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void markErrorFailure_doesNotMaskOperatorException() {
        when(vendorConfigService.getTimeoutInMillis("jdb")).thenReturn(4000);
        when(operatorCreditRequestMapper.toOperatorRequest(
                ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(new OperatorCreditRequest());
        when(operatorApiService.execute(
                ArgumentMatchers.any(CreditOperatorWalletAdapter.class),
                ArgumentMatchers.<OperatorCallContext<OperatorRequestObject>>any(),
                ArgumentMatchers.any()))
                .thenThrow(new RuntimeException("primary"));
        doThrow(new RuntimeException("markError db unreachable"))
                .when(creditTxnDataService).markError(ArgumentMatchers.any(), ArgumentMatchers.anyString());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> invoker.invoke(settleContext(), freshTxn(), WalletIdempotencyResolver.NO_CAS));

        assertEquals("primary", ex.getMessage(),
                "operator exception is rethrown even when markError itself failed");
    }

    @Test
    void markSuccessFailsAfterOperatorApplied_neverMarksErrorAndStillReportsSuccess() {
        // The double-credit path. markError puts the doc in the ERROR re-arm state, so marking a
        // credit the operator ALREADY applied lets the next vendor retry re-arm it, claim it via
        // markSent (a sequential retry presents a matching CAS, so the CAS guard does not help)
        // and credit the player a second time.
        when(vendorConfigService.getTimeoutInMillis("jdb")).thenReturn(4000);
        when(operatorCreditRequestMapper.toOperatorRequest(
                ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(new OperatorCreditRequest());
        when(operatorApiService.execute(
                ArgumentMatchers.any(CreditOperatorWalletAdapter.class),
                ArgumentMatchers.<OperatorCallContext<OperatorRequestObject>>any(),
                ArgumentMatchers.any()))
                .thenReturn(new PlayerBalanceData("op-bob", "PHP", new BigDecimal("106.50"), 0L));
        doThrow(new RuntimeException("couchbase blip"))
                .when(creditTxnDataService).markSuccess(ArgumentMatchers.any(), ArgumentMatchers.any());

        CreditTxn txn = freshTxn();
        PlayerBalanceData result = invoker.invoke(settleContext(), txn, WalletIdempotencyResolver.NO_CAS);

        // Vendor is told success — the credit really was applied, so reporting failure here is the
        // reconciliation mismatch we are trying to prevent, and it would also trigger a retry.
        assertEquals(0, new BigDecimal("106.50").compareTo(result.getBalance()));
        verify(creditTxnDataService, never()).markError(
                ArgumentMatchers.any(), ArgumentMatchers.anyString());
    }

    @Test
    void reArm_casMismatchOnMarkSent_throwsDuplicateAndNeverCallsOperator() {
        // Two concurrent vendor retries of the same ERROR'd txn both read ERROR and both proceed.
        // markSent is CAS-guarded on that path, so exactly one wins; the loser must abort rather
        // than call the operator a second time and double-credit the player.
        CreditTxn txn = freshTxn();
        doThrow(new CasMismatchException(null))
                .when(creditTxnDataService).markSent(ArgumentMatchers.eq(txn), ArgumentMatchers.eq(77L));

        assertThrows(DuplicateRequestException.class,
                () -> invoker.invoke(settleContext(), txn, 77L));

        verify(operatorApiService, never()).execute(
                ArgumentMatchers.any(CreditOperatorWalletAdapter.class),
                ArgumentMatchers.<OperatorCallContext<OperatorRequestObject>>any(),
                ArgumentMatchers.any());
        verify(creditTxnDataService, never()).markSuccess(ArgumentMatchers.any(), ArgumentMatchers.any());
        verify(creditTxnDataService, never()).markError(ArgumentMatchers.any(), ArgumentMatchers.anyString());
    }
}
