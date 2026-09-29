package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.idempotency.DuplicateRequestGuard;
import com.nextgen.gameaggregator.core.idempotency.RequestIdempotencyService;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.entity.promo.Campaign;
import com.nextgen.gameaggregator.enums.PromoType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The idempotency record is written before any work happens, so a failed payout must release it —
 * otherwise the vendor's retry is answered "already processed" for a payout that never landed.
 *
 * <p>The line is drawn at the operator call: once the payout may have gone out, the key stays claimed
 * even if something later fails, because freeing it would risk paying twice.
 */
@ExtendWith(MockitoExtension.class)
class PromoPayoutServiceImplIdempotencyTest {

    @Mock
    private DuplicateRequestGuard guard;
    @Mock
    private PromoPayoutContextEnricher enricher;
    @Mock
    private PromoPayoutValidator validator;
    @Mock
    private PromoPayoutProcessor processor;
    @Mock
    private RequestIdempotencyService requestIdempotencyService;

    @InjectMocks
    private PromoPayoutServiceImpl service;

    private PromoPayoutContext context;

    @BeforeEach
    void setUp() {
        LogContext logContext = new LogContext();
        logContext.setVendorClassName("digitainPromoWin");
        LogContextHolder.set(logContext);

        context = PromoPayoutContext.builder()
                .idempotencyKey("ga_7790436526_89")
                .vendorTransactionId("ga_7790436526_89")
                .vendorPlayerUsername("1e9be8j5dsug")
                .vendorCurrency("BRL")
                .vendorPayoutAmount(BigDecimal.valueOf(20))
                .promoType(PromoType.TOURNAMENT)
                .build();
    }

    @AfterEach
    void tearDown() {
        LogContextHolder.clear();
        PromoPayoutContextHolder.clear();
    }

    @Test
    void releasesIdempotencyKeyWhenEnrichmentFails() {
        service.initialise(context);
        doThrow(new EntityNotFoundException(Campaign.class, "vendorCampaignCode", "459713"))
                .when(enricher).enrich(context);

        assertThatThrownBy(() -> service.process(context))
                .isInstanceOf(EntityNotFoundException.class);

        verify(guard).clear();
        verify(processor, never()).process(any());
    }

    @Test
    void releasesIdempotencyKeyWhenValidationFails() {
        service.initialise(context);
        doThrow(new InternalConfigurationException("promoType is required"))
                .when(validator).validateOrThrow(context);

        assertThatThrownBy(() -> service.process(context))
                .isInstanceOf(InternalConfigurationException.class);

        verify(guard).clear();
        verify(processor, never()).process(any());
    }

    /**
     * The record belongs to the original request, not this one, so it must survive. Deleting it would
     * un-guard a payout that really did happen and let the next resend pay the player twice.
     */
    @Test
    void doesNotReleaseIdempotencyKeyOnDuplicate() {
        service.initialise(context);
        doThrow(new DuplicateRequestException("already processed"))
                .when(guard).ensureNotDuplicate(anyString(), anyString(), anyString());

        assertThatThrownBy(() -> service.process(context))
                .isInstanceOf(DuplicateRequestException.class);

        verify(guard, never()).clear();
        verify(enricher, never()).enrich(any());
    }

    /**
     * The processor marks the context the moment it hands the payout to the operator. A throw after that
     * point — still inside process() — must not free the key, even though process() never returned.
     */
    @Test
    void keepsIdempotencyKeyWhenProcessorThrowsAfterDispatchingToTheOperator() {
        service.initialise(context);
        when(processor.process(context)).thenAnswer(invocation -> {
            invocation.getArgument(0, PromoPayoutContext.class).setPayoutDispatched(true);
            throw new IllegalStateException("balance conversion failed after the operator was called");
        });

        assertThatThrownBy(() -> service.process(context))
                .isInstanceOf(IllegalStateException.class);

        verify(guard, never()).clear();
    }

    /**
     * Once the operator has been called the payout may have landed, so a failure in the bookkeeping that
     * follows must not free the key — a retry would pay the player a second time.
     */
    @Test
    void keepsIdempotencyKeyWhenFailureIsAfterTheOperatorCall() {
        service.initialise(context);
        when(processor.process(context))
                .thenReturn(new PlayerBalanceData("1e9be8j5dsug", "BRL", BigDecimal.valueOf(520), 1L));
        doThrow(new RuntimeException("couchbase down"))
                .when(requestIdempotencyService).enrichIdempotentLog(any(), any(), any());

        assertThatThrownBy(() -> service.process(context))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("couchbase down");

        verify(guard, never()).clear();
    }

    /**
     * The balance stored here is what a later duplicate is answered with, so a successful payout must
     * leave the real one behind — not the zero the record is created with.
     */
    @Test
    void keepsIdempotencyKeyOnSuccessAndStoresTheBalance() {
        service.initialise(context);
        // mirrors PromoPayoutContextEnricher.prepareContext, which mints the internal transaction id
        doAnswer(invocation -> {
            invocation.getArgument(0, PromoPayoutContext.class).setTransactionId("01a0556f53bb7e0c");
            return null;
        }).when(enricher).enrich(context);
        when(processor.process(context))
                .thenReturn(new PlayerBalanceData("1e9be8j5dsug", "BRL", BigDecimal.valueOf(520), 1L));

        service.process(context);

        verify(guard, never()).clear();
        verify(requestIdempotencyService).enrichIdempotentLog("01a0556f53bb7e0c", "BRL", BigDecimal.valueOf(520));
    }

    @Test
    void keepsIdempotencyKeyOnceBatchIsSubscribed() {
        service.initialise(context);
        service.configure(config -> config.batch(true));
        when(processor.processBatch(context)).thenReturn(Mono.empty());

        doThrow(new RuntimeException("couchbase down"))
                .when(requestIdempotencyService).enrichIdempotentLog(any(), any(), any());

        assertThatThrownBy(() -> service.process(context))
                .isInstanceOf(RuntimeException.class);

        verify(guard, never()).clear();
    }
}
