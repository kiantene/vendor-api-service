package com.nextgen.gameaggregator.vendor.digitain.api.promowin;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.vendor.digitain.constant.ResponseCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PromoWinResponseMapperTest {

    private final PromoWinResponseMapper mapper = new PromoWinResponseMapper();

    @Test
    void buildsTheSuccessBody() {
        PromoPayoutContext context = PromoPayoutContext.builder()
                .vendorTransactionId("40439c1a12cbe8c68df1")
                .build();

        PromoWinResponse response = mapper.toVendor(context,
                PlayerBalanceData.getDefaultWithBalance("65441", "EUR", new BigDecimal("1987.21")));

        assertThat(response.getErr()).isEqualTo(ResponseCode.SUCCESS.code);
        assertThat(response.getTxid()).isEqualTo("40439c1a12cbe8c68df1");
        assertThat(response.getPid()).isEqualTo("65441");
        assertThat(response.getBln()).isEqualByComparingTo("1987.21");
    }

    /** Digitain quotes balances to 4dp, and truncates rather than rounds up. */
    @Test
    void reportsTheBalanceToFourDecimalPlaces() {
        PromoWinResponse response = mapper.toVendor(
                PromoPayoutContext.builder().vendorTransactionId("tx-1").build(),
                PlayerBalanceData.getDefaultWithBalance("65441", "EUR", new BigDecimal("10.123456")));

        assertThat(response.getBln()).isEqualTo(new BigDecimal("10.1234"));
    }
}
