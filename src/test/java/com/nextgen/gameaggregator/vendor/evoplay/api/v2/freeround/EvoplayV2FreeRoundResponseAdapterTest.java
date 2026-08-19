package com.nextgen.gameaggregator.vendor.evoplay.api.v2.freeround;

import com.nextgen.gameaggregator.vendor.evoplay.api.freeround.EvoplayFreeRoundPayoutResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EvoplayV2FreeRoundResponseAdapterTest {
    private final EvoplayV2FreeRoundResponseAdapter adapter = new EvoplayV2FreeRoundResponseAdapter();

    @Test
    void should_build_v2_success_response() {
        EvoplayFreeRoundPayoutResult result = EvoplayFreeRoundPayoutResult.builder()
                .balance(new BigDecimal("34.56"))
                .currency("USD")
                .build();

        var response = adapter.toV2(result);

        assertThat(response.getStatus()).isEqualTo("ok");
        assertThat(response.getData().getBalance()).isEqualByComparingTo("34.56");
        assertThat(response.getData().getCurrency()).isEqualTo("USD");
        assertThat(response.getError()).isNull();
    }
}
