package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Test
    void sendsPlayerIdInExtra() {
        PromoPayoutContext context = contextWith(PromoType.TOURNAMENT);
        context.getAgent().playerId(1234567L);

        PromoPayoutDto dto = mapper.toPromoPayoutRequest(context);

        assertThat(dto.getExtra()).isNotNull();
        assertThat(dto.getExtra().getPlayerId()).isEqualTo("1234567");
    }

    @Test
    void batchOverloadSendsPlayerIdFromTheContext() {
        // children carry no player of their own; username comes from the context, so playerId must too
        PromoPayoutContext context = contextWith(PromoType.JACKPOT);
        context.getAgent().playerId(1234567L);

        PromoPayoutDto dto = mapper.toPromoPayoutRequest(context, childTransaction());

        assertThat(dto.getExtra().getPlayerId()).isEqualTo("1234567");
    }

    @Test
    void omitsExtraEntirelyWhenThereIsNoPlayerId() {
        // better an absent key than "extra": {"playerId": null}
        PromoPayoutDto dto = mapper.toPromoPayoutRequest(contextWith(PromoType.TOURNAMENT));

        assertThat(dto.getExtra()).isNull();
    }

    @Test
    void serialisesExtraAsANestedObject() throws Exception {
        PromoPayoutContext context = contextWith(PromoType.TOURNAMENT);
        context.getAgent().playerId(1234567L);

        String json = new ObjectMapper().writeValueAsString(mapper.toPromoPayoutRequest(context));

        assertThat(json).contains("\"extra\":{\"playerId\":\"1234567\"}");
    }

    @Test
    void omittedExtraIsAbsentFromTheJson() throws Exception {
        String json = new ObjectMapper().writeValueAsString(
                mapper.toPromoPayoutRequest(contextWith(PromoType.TOURNAMENT)));

        assertThat(json).doesNotContain("extra");
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
