package com.nextgen.gameaggregator.core.engine.promo;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutMapper;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class PromoPayoutMapperTest {

    private final PromoPayoutMapper mapper = Mappers.getMapper(PromoPayoutMapper.class);

    @Test
    @DisplayName("Should correctly map PromoPayoutContext to PromoPayoutRequest")
    void shouldMapContextToRequest() {

        String expectedTraceId = "GLOBAL-TRACE-XYZ";
        String expectedUsername = "vendorUser123";
        String expectedTransactionId = "INTERNAL-TXN-ABC";
        String expectedCurrency = "USD";
        BigDecimal expectedAmount = new BigDecimal("75.25");
        String expectedType = "BONUS";
        Long expectedTimestamp = Instant.now().toEpochMilli();

        PromoPayoutContext context = PromoPayoutContext.builder()
                .idempotencyKey("idem-key-1")
                .vendorPlayerUsername(expectedUsername)
                .vendorCurrency("SGD")
                .amount(expectedAmount)
                .timestamp(expectedTimestamp)
                .traceId(expectedTraceId)
                .transactionId(expectedTransactionId)
                .currency(expectedCurrency)
                .type(expectedType)
                .vendorClassName("pgsoft")
                .build();

        PromoPayoutRequest request = mapper.toPromoPayoutRequest(context);

        assertThat(request).isNotNull();
        assertThat(request.getTraceId()).isEqualTo(expectedTraceId);
        assertThat(request.getUsername()).isEqualTo(expectedUsername);
        assertThat(request.getTransactionId()).isEqualTo(expectedTransactionId);
        assertThat(request.getCurrency()).isEqualTo(expectedCurrency);
        assertThat(request.getAmount()).isEqualTo(expectedAmount);
        assertThat(request.getType()).isEqualTo(expectedType);
        assertThat(request.getTimestamp()).isEqualTo(expectedTimestamp);
    }

    @Test
    @DisplayName("Should handle null PromoPayoutContext gracefully")
    void shouldHandleNullContext() {
        PromoPayoutRequest request = mapper.toPromoPayoutRequest(null);
        assertThat(request).isNull();
    }

    @Test
    @DisplayName("Should map fields even if some are null in context (where @NotNull on request would fail validation later)")
    void shouldMapWithNullsInContext() {

        PromoPayoutContext contextWithNulls = PromoPayoutContext.builder()
                .idempotencyKey("idem-1")
                .vendorPlayerUsername("user")
                .vendorCurrency("AUD")
                .amount(new BigDecimal("10.00"))
                .timestamp(Instant.now().toEpochMilli())
                .traceId(null)
                .transactionId("txn-1")
                .currency("AUD")
                .type("REFUND")
                .vendorClassName("com.example.Vendor")
                .build();

        PromoPayoutRequest request = mapper.toPromoPayoutRequest(contextWithNulls);

        assertThat(request).isNotNull();
        assertThat(request.getTraceId()).isNull();
        assertThat(request.getUsername()).isEqualTo("user");
    }
}
