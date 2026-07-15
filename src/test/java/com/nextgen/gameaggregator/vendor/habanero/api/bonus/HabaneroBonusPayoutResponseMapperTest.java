package com.nextgen.gameaggregator.vendor.habanero.api.bonus;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.TransferVo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class HabaneroBonusPayoutResponseMapperTest {

    private final HabaneroBonusPayoutResponseMapper mapper = new HabaneroBonusPayoutResponseMapper();

    @Test
    void toVendor_setsSuccessResponseCode() {
        PromoPayoutContext context = PromoPayoutContext.builder().vendorCurrency("PHP").build();
        PlayerBalanceData balanceData = new PlayerBalanceData("player01", "PHP", BigDecimal.TEN, System.currentTimeMillis());

        TransferVo response = mapper.toVendor(context, balanceData);

        assertThat(response.getFundTransferResponseVo().getStatusVo().getSuccess()).isTrue();
    }

    @Test
    void toVendor_mapsBalanceScaledDownTo2Decimals() {
        PromoPayoutContext context = PromoPayoutContext.builder().vendorCurrency("USD").build();
        PlayerBalanceData balanceData = new PlayerBalanceData("player01", "USD", new BigDecimal("99.999"), System.currentTimeMillis());

        TransferVo response = mapper.toVendor(context, balanceData);

        assertThat(response.getFundTransferResponseVo().getBalance()).isEqualByComparingTo("99.99");
    }

    @Test
    void toVendor_mapsCurrencyCodeFromContext() {
        PromoPayoutContext context = PromoPayoutContext.builder().vendorCurrency("CNY").build();
        PlayerBalanceData balanceData = new PlayerBalanceData("player01", "CNY", BigDecimal.TEN, System.currentTimeMillis());

        TransferVo response = mapper.toVendor(context, balanceData);

        assertThat(response.getFundTransferResponseVo().getCurrencyCode()).isEqualTo("CNY");
    }
}
