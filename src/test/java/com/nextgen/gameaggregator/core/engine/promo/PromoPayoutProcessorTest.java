package com.nextgen.gameaggregator.core.engine.promo;

import com.nextgen.gameaggregator.core.common.ClientRequestService;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PayoutTransaction;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutMapper;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutProcessor;
import com.nextgen.gameaggregator.core.retry.RetryQueueService;
import com.nextgen.gameaggregator.core.webclient.OperatorApiAdapter;
import com.nextgen.gameaggregator.core.webclient.OperatorReactiveApiAdapter;
import com.nextgen.core.api.ApiResult;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutDto;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.webclient.ClientApiResponse;
import com.nextgen.gameaggregator.core.webclient.OperatorApiRequest;
import com.nextgen.gameaggregator.enums.PromoType;
import com.nextgen.gameaggregator.service.data.producer.PromoPayoutHistoryProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PromoPayoutProcessorTest {

    @Mock private PromoPayoutMapper mapper;
    @Mock private ClientRequestService clientRequestService;
    @Mock private OperatorApiAdapter operatorApiAdapter;
    @Mock private OperatorReactiveApiAdapter reactiveApiAdapter;
    @Mock private PromoPayoutHistoryProducer producer;
    @Mock private RetryQueueService retryQueueService;

    @InjectMocks
    private PromoPayoutProcessor processor;

    @BeforeEach
    void setUp() {
        LogContextHolder.set(new LogContext());
    }

    @AfterEach
    void tearDown() {
        LogContextHolder.clear();
    }

    @Test
    void process_zeroAmount_publishesToHistory() {
        PromoPayoutContext context = contextWithAmount(BigDecimal.ZERO);
        stubOperatorCall(context);

        processor.process(context);

        verify(producer, times(1)).publish(context);
    }

    @Test
    void process_positiveAmount_publishesToHistory() {
        PromoPayoutContext context = contextWithAmount(new BigDecimal("100.00"));
        stubOperatorCall(context);

        processor.process(context);

        verify(producer, times(1)).publish(context);
    }

    @Test
    void process_nullAmount_doesNotPublishAndNoNPE() {
        PromoPayoutContext context = contextWithAmount(null);
        stubOperatorCall(context);

        assertThatCode(() -> processor.process(context)).doesNotThrowAnyException();

        verify(producer, never()).publish(any(PromoPayoutContext.class));
    }

    @Test
    void process_negativeAmount_doesNotPublish() {
        PromoPayoutContext context = contextWithAmount(new BigDecimal("-10.00"));
        stubOperatorCall(context);

        processor.process(context);

        verify(producer, never()).publish(any(PromoPayoutContext.class));
    }

    @Test
    void processBatch_zeroAndPositiveAmounts_published() {
        PromoPayoutContext context = contextWithBatchTransactions(
                new BigDecimal("50.00"),
                BigDecimal.ZERO,
                new BigDecimal("100.00")
        );
        stubBatchOperatorCall();

        processor.processBatch(context).block();

        verify(producer, times(3)).publish(any(PromoPayoutContext.class), any(PayoutTransaction.class));
    }

    @Test
    void processBatch_nullAndNegativeAmounts_notPublished() {
        PromoPayoutContext context = contextWithBatchTransactions(
                null,
                new BigDecimal("-5.00")
        );
        stubBatchOperatorCall();

        processor.processBatch(context).block();

        verify(producer, never()).publish(any(PromoPayoutContext.class), any(PayoutTransaction.class));
    }

    private PromoPayoutContext contextWithAmount(BigDecimal amount) {
        return PromoPayoutContext.builder()
                .vendorPlayerUsername("testplayer")
                .vendorCurrency("PHP")
                .vendorPayoutAmount(amount)
                .vendorCampaignCode("campaign-001")
                .vendorTransactionId("txn-001")
                .promoType(PromoType.FREE_ROUND)
                .build();
    }

    private PromoPayoutContext contextWithBatchTransactions(BigDecimal... amounts) {
        List<PayoutTransaction> transactions = new ArrayList<>();
        for (BigDecimal amount : amounts) {
            PayoutTransaction tx = PayoutTransaction.builder()
                    .vendorPayoutAmount(amount)
                    .vendorTransactionId("txn-" + amount)
                    .vendorCampaignCode("campaign-001")
                    .vendorCurrency("PHP")
                    .vendorPlayerUsername("testplayer")
                    .build();
            transactions.add(tx);
        }
        return PromoPayoutContext.builder()
                .vendorPlayerUsername("testplayer")
                .vendorCurrency("PHP")
                .vendorCampaignCode("campaign-001")
                .promoType(PromoType.FREE_ROUND)
                .payoutTransactions(transactions)
                .build();
    }

    private void stubBatchOperatorCall() {
        PromoPayoutDto dto = PromoPayoutDto.builder().timestamp(System.currentTimeMillis()).build();
        OperatorApiRequest mockApiRequest = mock(OperatorApiRequest.class);
        // getPartitionKey() must be non-null since RetryHelper.toHttpCallSpec() calls hashCode() on it
        when(mockApiRequest.getPartitionKey()).thenReturn("testplayer");

        when(mapper.toPromoPayoutRequest(any(PromoPayoutContext.class), any(PayoutTransaction.class))).thenReturn(dto);
        when(clientRequestService.createOperatorApiRequest(any(), any(), any(), any(), any(), any()))
                .thenReturn(mockApiRequest);
        when(reactiveApiAdapter.execute(any(OperatorApiRequest.class), any(LogContext.class)))
                .thenReturn(Mono.<ApiResult>error(new RuntimeException("mock operator error")));
        when(retryQueueService.enqueue(any(), any())).thenReturn(Mono.empty());
    }

    private void stubOperatorCall(PromoPayoutContext context) {
        PlayerBalanceData balance = PlayerBalanceData.getDefault(
                context.getVendorPlayerUsername(),
                context.getVendorCurrency()
        );
        ClientApiResponse clientApiResponse = new ClientApiResponse();
        clientApiResponse.setData(balance);
        when(clientRequestService.shouldMockResponse(any())).thenReturn(true);
        when(clientRequestService.mockClientResponse(any(), any(), any())).thenReturn(clientApiResponse);
    }
}
