package com.nextgen.gameaggregator.vendor.evolutionv2.api.promo;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.vendor.evolution.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.evolution.vo.ResponseVo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Evolution v2 promo-payout integration.
 */
class PromoPayoutResponseMapperTest {

    private final PromoPayoutResponseMapper mapper = new PromoPayoutResponseMapper();

    @Test
    void toVendor_returnsBalanceOnlySuccessResponse() {
        PlayerBalanceData balance = new PlayerBalanceData(
                "player123",
                "USD",
                new BigDecimal("999.35"),
                1754373936436L
        );

        ResponseVo response = mapper.toVendor(
                EvolutionPromoPayoutContext.builder()
                        .vendorRequestUuid("request-123")
                        .build(),
                balance
        );

        assertThat(response.getResponseCode()).isEqualTo(ResponseCode.OK);
        assertThat(response.getStatus()).isEqualTo(ResponseCode.OK.status);
        assertThat(response.getBalance()).isEqualByComparingTo("999.35");
        assertThat(response.getBonus()).isNull();
        assertThat(response.getUuid()).isEqualTo("request-123");
    }
}
