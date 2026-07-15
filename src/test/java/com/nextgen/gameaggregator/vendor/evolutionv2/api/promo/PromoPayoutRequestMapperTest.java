package com.nextgen.gameaggregator.vendor.evolutionv2.api.promo;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.enums.PromoType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Evolution v2 promo-payout integration.
 */
class PromoPayoutRequestMapperTest {

    private final PromoPayoutRequestMapper mapper = new PromoPayoutRequestMapper();

    @ParameterizedTest
    @ValueSource(strings = {
            "FreeRoundPlayableSpent",
            "JackpotWin",
            "RtrMonetaryReward",
            "SmartTournamentMonetaryReward",
            "CashReward"
    })
    void toInternal_mapsAllPromoTransactionTypesTypeAgnostically(String transactionType) {
        PromoPayoutRequestDto request = request(transactionType, "voucher-123");

        PromoPayoutContext context = mapper.toInternal(request);

        assertThat(context.getIdempotencyKey()).isEqualTo("promo-tx-123");
        assertThat(context.getVendorTransactionId()).isEqualTo("promo-tx-123");
        assertThat(context.getVendorSessionToken()).isEqualTo("sid-123");
        assertThat(context.getVendorPlayerUsername()).isEqualTo("player123");
        assertThat(context.getVendorCurrency()).isEqualTo("USD");
        assertThat(context.getVendorCampaignCode()).isEqualTo("voucher123");
        assertThat(context.getVendorPayoutAmount()).isEqualByComparingTo("12.345678");
        assertThat(context.getVendorTransactionTime()).isNull();
        assertThat(context.getPromoType()).isEqualTo(PromoType.FREE_ROUND);
        assertThat(context).isInstanceOf(EvolutionPromoPayoutContext.class);
        assertThat(((EvolutionPromoPayoutContext) context).getVendorRequestUuid()).isEqualTo("request-123");
    }

    @Test
    void toInternal_stripsHyphensFromVoucherId() {
        PromoPayoutRequestDto request = request("FreeRoundPlayableSpent", "fa4ef1b5-9d2c-444e-aeff-07611fbeac91");

        PromoPayoutContext context = mapper.toInternal(request);

        assertThat(context.getVendorCampaignCode()).isEqualTo("fa4ef1b59d2c444eaeff07611fbeac91");
    }

    private PromoPayoutRequestDto request(String transactionType, String voucherId) {
        PromoTransactionDto transaction = new PromoTransactionDto();
        transaction.setType(transactionType);
        transaction.setId("promo-tx-123");
        transaction.setAmount(new BigDecimal("12.345678"));
        transaction.setVoucherId(voucherId);

        PromoPayoutRequestDto request = new PromoPayoutRequestDto();
        request.setSid("sid-123");
        request.setUserId("player123");
        request.setCurrency("USD");
        request.setUuid("request-123");
        request.setPromoTransaction(transaction);
        return request;
    }
}
