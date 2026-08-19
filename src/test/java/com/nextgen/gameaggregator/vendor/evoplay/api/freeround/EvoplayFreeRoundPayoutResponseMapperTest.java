package com.nextgen.gameaggregator.vendor.evoplay.api.freeround;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EvoplayFreeRoundPayoutResponseMapperTest {
    private final EvoplayFreeRoundPayoutResponseMapper mapper = new EvoplayFreeRoundPayoutResponseMapper();
    private final EvoplayFreeRoundResponseAdapter v1Adapter = new EvoplayFreeRoundResponseAdapter();

    @Test
    void should_map_player_balance_to_common_result() {
        PromoPayoutContext context = PromoPayoutContext.builder()
                .vendorCurrency("USD")
                .build();
        PlayerBalanceData balanceData = new PlayerBalanceData("player-1", "CNY", new BigDecimal("99.99"), 1L);

        EvoplayFreeRoundPayoutResult result = mapper.toVendor(context, balanceData);

        assertThat(result.getBalance()).isEqualByComparingTo("99.99");
        assertThat(result.getCurrency()).isEqualTo("CNY");
    }

    @Test
    void should_fallback_to_zero_balance_and_context_currency() {
        PromoPayoutContext context = PromoPayoutContext.builder()
                .vendorCurrency("USD")
                .build();

        EvoplayFreeRoundPayoutResult result = mapper.toVendor(context, null);

        assertThat(result.getBalance()).isZero();
        assertThat(result.getCurrency()).isEqualTo("USD");
    }

    @Test
    void should_build_v1_success_response() {
        EvoplayFreeRoundPayoutResult result = EvoplayFreeRoundPayoutResult.builder()
                .balance(new BigDecimal("12.00"))
                .currency("USD")
                .build();

        var response = v1Adapter.toV1(result);

        assertThat(response.getStatus()).isEqualTo("ok");
        assertThat(response.getData().getBalance()).isEqualByComparingTo("12.00");
        assertThat(response.getData().getCurrency()).isEqualTo("USD");
        assertThat(response.getError()).isNull();
    }
}
