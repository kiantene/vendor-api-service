package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.enums.PromoType;
import com.nextgen.gameaggregator.service.data.model.TxnAmount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PromoPayoutMapperTest {

    private static final long TXN_TIME = 1_700_000_000_000L;

    private final PromoPayoutMapper mapper = new PromoPayoutMapper();

    private PromoPayoutContext contextWith(PromoType promoType) {
        PromoPayoutContext context = PromoPayoutContext.builder().build();
        context.setPromoType(promoType);
        context.setTraceId("trace-1");
        context.setTransactionId("txn-1");
        context.setCampaignUuid("campaign-1");
        context.setCurrencyCode("THB");
        context.setPayout(TxnAmount.of(BigDecimal.TEN, BigDecimal.ONE));
        context.setVendorTransactionTime(TXN_TIME);
        context.getAgent().playerUsername("player1");
        return context;
    }

    private PayoutTransaction childTransaction() {
        PayoutTransaction txn = PayoutTransaction.builder().build();
        txn.setTraceId("trace-1");
        txn.setTransactionId("child-txn-1");
        txn.setPayout(TxnAmount.of(BigDecimal.ONE, BigDecimal.ONE));
        txn.setVendorTransactionTime(TXN_TIME);
        return txn;
    }

    @Test
    void singleOverloadSendsPromoTypeCode() {
        PromoPayoutDto dto = mapper.toPromoPayoutRequest(contextWith(PromoType.TOURNAMENT));

        assertThat(dto.getPromoType()).isEqualTo("TOURNAMENT");
        assertThat(dto.getTraceId()).isEqualTo("trace-1");
        assertThat(dto.getUsername()).isEqualTo("player1");
        assertThat(dto.getCampaignId()).isEqualTo("campaign-1");
    }

    @Test
    void batchOverloadSendsPromoTypeCodeFromTheContext() {
        // promoType lives on the context; batch children carry only their own txn/amount/time
        PromoPayoutDto dto = mapper.toPromoPayoutRequest(contextWith(PromoType.JACKPOT), childTransaction());

        assertThat(dto.getPromoType()).isEqualTo("JACKPOT");
        assertThat(dto.getTransactionId()).isEqualTo("child-txn-1");
    }

    @ParameterizedTest
    @EnumSource(PromoType.class)
    void everyPromoTypeIsSentAsItsCode(PromoType promoType) {
        PromoPayoutContext context = contextWith(promoType);

        assertThat(mapper.toPromoPayoutRequest(context).getPromoType())
                .isEqualTo(promoType.code);
        assertThat(mapper.toPromoPayoutRequest(context, childTransaction()).getPromoType())
                .isEqualTo(promoType.code);
    }

    @Test
    void nullContextStillReturnsNull() {
        assertThat(mapper.toPromoPayoutRequest(null)).isNull();
        assertThat(mapper.toPromoPayoutRequest(null, childTransaction())).isNull();
    }
}
