package com.nextgen.gameaggregator.core.engine.wallet.debit;

import com.couchbase.client.core.error.CasMismatchException;
import com.nextgen.gameaggregator.core.common.OperatorRequestObject;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.idempotency.WalletIdempotencyResolver;
import com.nextgen.gameaggregator.core.engine.operator.OperatorApiService;
import com.nextgen.gameaggregator.core.engine.operator.OperatorCallContext;
import com.nextgen.gameaggregator.core.engine.operator.OperatorScenario;
import com.nextgen.gameaggregator.core.engine.operator.wallet.debit.DebitOperatorWalletAdapter;
import com.nextgen.gameaggregator.core.engine.operator.wallet.debit.OperatorDebitRequest;
import com.nextgen.gameaggregator.core.engine.operator.wallet.debit.OperatorDebitRequestMapper;
import com.nextgen.gameaggregator.core.vendor.config.VendorConfigService;
import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.service.data.DebitTxnDataService;
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
class DebitOperatorInvokerTest {

    @Mock private OperatorApiService operatorApiService;
    @Mock private OperatorDebitRequestMapper operatorDebitRequestMapper;
    @Mock private DebitOperatorWalletAdapter debitOperatorWalletAdapter;
    @Mock private VendorConfigService vendorConfigService;
    @Mock private DebitTxnDataService debitTxnDataService;

    @InjectMocks private DebitOperatorInvoker invoker;

    private DebitContext context() {
        return DebitContext.builder()
                .vendorTxnId("transfer-1")
                .vendorClassName("jdb")
                .vendorPlayerUsername("bob")
                .vendorRoundId("round-1")
                .vendorId(88)
                .vendorCurrency("PHP")
                .currencyCode("PHP")
                .agentId(1)
                .agentPlayerUsername("op-bob")
                .token("session-token")
                .fromVendorRate(BigDecimal.ONE)
                .toVendorRate(BigDecimal.ONE)
                .build();
    }

    private DebitTxn freshTxn() {
        return DebitTxn.of("jdb", "transfer-1", 1L);
    }

    @Test
    void happy_marksSentThenSuccessAndReturnsVendorView() {
        when(vendorConfigService.getTimeoutInMillis("jdb")).thenReturn(4000);
        when(operatorDebitRequestMapper.toOperatorRequest(
                ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(new OperatorDebitRequest());
        when(operatorApiService.execute(
                ArgumentMatchers.eq(debitOperatorWalletAdapter),
                ArgumentMatchers.<OperatorCallContext<OperatorRequestObject>>any(),
                ArgumentMatchers.any()))
                .thenReturn(new PlayerBalanceData("bob", "PHP", new BigDecimal("95.00"), 0L));

        DebitTxn txn = freshTxn();
        PlayerBalanceData result = invoker.invoke(context(), txn, WalletIdempotencyResolver.NO_CAS);

        assertEquals(0, new BigDecimal("95.00").compareTo(result.getBalance()));
        verify(debitTxnDataService).markSent(txn, WalletIdempotencyResolver.NO_CAS);
        verify(debitTxnDataService).markSuccess(
                ArgumentMatchers.eq(txn),
                argThat(b -> b != null && b.compareTo(new BigDecimal("95.00")) == 0));
        verify(debitTxnDataService, never()).markError(
                ArgumentMatchers.any(), ArgumentMatchers.anyString());
    }

    @Test
    void dispatchesToDebitEndpointWithTargetBuiltFromContext() {
        when(vendorConfigService.getTimeoutInMillis("jdb")).thenReturn(4000);
        when(operatorDebitRequestMapper.toOperatorRequest(
                ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(new OperatorDebitRequest());
        when(operatorApiService.execute(
                ArgumentMatchers.any(DebitOperatorWalletAdapter.class),
                ArgumentMatchers.<OperatorCallContext<OperatorRequestObject>>any(),
                ArgumentMatchers.any()))
                .thenReturn(new PlayerBalanceData("bob", "PHP", new BigDecimal("1"), 0L));

        invoker.invoke(context(), freshTxn(), WalletIdempotencyResolver.NO_CAS);

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<OperatorCallContext<OperatorRequestObject>> cap =
                (ArgumentCaptor) ArgumentCaptor.forClass(OperatorCallContext.class);
        verify(operatorApiService).execute(
                ArgumentMatchers.eq(debitOperatorWalletAdapter), cap.capture(), ArgumentMatchers.any());
        OperatorCallContext<OperatorRequestObject> ctx = cap.getValue();
        assertEquals(EndPoints.WALLET_BET_DEBIT, ctx.endpoint());
        assertEquals(Integer.valueOf(1), ctx.target().agentId());
        assertEquals("op-bob", ctx.target().agentPlayerUsername());
        assertEquals("PHP", ctx.target().currency());
        assertEquals("session-token", ctx.target().sessionToken());
    }

    @Test
    void operatorThrows_marksErrorAndRethrows() {
        when(vendorConfigService.getTimeoutInMillis("jdb")).thenReturn(4000);
        when(operatorDebitRequestMapper.toOperatorRequest(
                ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(new OperatorDebitRequest());
        when(operatorApiService.execute(
                ArgumentMatchers.any(DebitOperatorWalletAdapter.class),
                ArgumentMatchers.<OperatorCallContext<OperatorRequestObject>>any(),
                ArgumentMatchers.any()))
                .thenThrow(new RuntimeException("boom"));

        DebitTxn txn = freshTxn();
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> invoker.invoke(context(), txn, WalletIdempotencyResolver.NO_CAS));

        assertEquals("boom", ex.getMessage());
        verify(debitTxnDataService).markSent(txn, WalletIdempotencyResolver.NO_CAS);
        verify(debitTxnDataService).markError(
                ArgumentMatchers.eq(txn), ArgumentMatchers.eq("RuntimeException"));
        verify(debitTxnDataService, never()).markSuccess(
                ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void markErrorFailure_doesNotMaskOperatorException() {
        when(vendorConfigService.getTimeoutInMillis("jdb")).thenReturn(4000);
        when(operatorDebitRequestMapper.toOperatorRequest(
                ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(new OperatorDebitRequest());
        when(operatorApiService.execute(
                ArgumentMatchers.any(DebitOperatorWalletAdapter.class),
                ArgumentMatchers.<OperatorCallContext<OperatorRequestObject>>any(),
                ArgumentMatchers.any()))
                .thenThrow(new RuntimeException("primary"));
        doThrow(new RuntimeException("markError db unreachable"))
                .when(debitTxnDataService).markError(ArgumentMatchers.any(), ArgumentMatchers.anyString());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> invoker.invoke(context(), freshTxn(), WalletIdempotencyResolver.NO_CAS));
        assertEquals("primary", ex.getMessage());
    }

    @Test
    void markSuccessFailsAfterOperatorApplied_neverMarksErrorAndStillReportsSuccess() {
        // Mirror of the credit case, with a different tail. Before the fix this propagated and left
        // the doc at SENT — and WalletIdempotencyResolver treats SENT as in-flight and always
        // throws DuplicateRequestException regardless of returnSuccessOnDuplicate, so every JDB
        // Action 9 retry came back DUPLICATE_TRANSACTION forever while the money had moved.
        when(vendorConfigService.getTimeoutInMillis("jdb")).thenReturn(4000);
        when(operatorDebitRequestMapper.toOperatorRequest(
                ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(new OperatorDebitRequest());
        when(operatorApiService.execute(
                ArgumentMatchers.any(DebitOperatorWalletAdapter.class),
                ArgumentMatchers.<OperatorCallContext<OperatorRequestObject>>any(),
                ArgumentMatchers.any()))
                .thenReturn(new PlayerBalanceData("bob", "PHP", new BigDecimal("95.00"), 0L));
        doThrow(new RuntimeException("couchbase blip"))
                .when(debitTxnDataService).markSuccess(ArgumentMatchers.any(), ArgumentMatchers.any());

        DebitTxn txn = freshTxn();
        PlayerBalanceData result = invoker.invoke(context(), txn, WalletIdempotencyResolver.NO_CAS);

        assertEquals(0, new BigDecimal("95.00").compareTo(result.getBalance()));
        verify(debitTxnDataService, never()).markError(
                ArgumentMatchers.any(), ArgumentMatchers.anyString());
    }

    @Test
    void reArm_casMismatchOnMarkSent_throwsDuplicateAndNeverCallsOperator() {
        // Two concurrent vendor retries of the same ERROR'd txn both read ERROR and both proceed.
        // markSent is CAS-guarded on that path, so exactly one wins; the loser must abort rather
        // than call the operator a second time and double-debit the player.
        DebitTxn txn = freshTxn();
        doThrow(new CasMismatchException(null))
                .when(debitTxnDataService).markSent(ArgumentMatchers.eq(txn), ArgumentMatchers.eq(77L));

        assertThrows(DuplicateRequestException.class,
                () -> invoker.invoke(context(), txn, 77L));

        verify(operatorApiService, never()).execute(
                ArgumentMatchers.any(DebitOperatorWalletAdapter.class),
                ArgumentMatchers.<OperatorCallContext<OperatorRequestObject>>any(),
                ArgumentMatchers.any());
        verify(debitTxnDataService, never()).markSuccess(ArgumentMatchers.any(), ArgumentMatchers.any());
        verify(debitTxnDataService, never()).markError(ArgumentMatchers.any(), ArgumentMatchers.anyString());
    }
}
