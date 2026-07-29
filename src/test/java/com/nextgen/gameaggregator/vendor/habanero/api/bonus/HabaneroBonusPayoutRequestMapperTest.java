package com.nextgen.gameaggregator.vendor.habanero.api.bonus;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.enums.PromoType;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.BonusDetailDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundInfoDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundTransferRequestDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class HabaneroBonusPayoutRequestMapperTest {

    private final HabaneroBonusPayoutRequestMapper mapper = new HabaneroBonusPayoutRequestMapper();

    @Test
    void toInternal_mapsAllFieldsCorrectly() {
        FundInfoDto fundInfo = new FundInfoDto();
        fundInfo.setTransferId("transfer-001");
        fundInfo.setAmount(new BigDecimal("50.00"));
        fundInfo.setCurrencyCode("USD");
        fundInfo.setDtEvent("2026-07-07T10:00:00");

        BonusDetailDto bonusDetails = new BonusDetailDto();
        bonusDetails.setCouponId("coupon-abc");

        FundTransferRequestDto fundTransferRequest = new FundTransferRequestDto();
        fundTransferRequest.setAccountId("player01");
        fundTransferRequest.setToken("token-xyz");

        HabaneroBonusPayoutRequest request = HabaneroBonusPayoutRequest.builder()
                .fundTransferRequest(fundTransferRequest)
                .fundInfo(fundInfo)
                .bonusDetails(bonusDetails)
                .vendorGameCode("game-001")
                .build();

        PromoPayoutContext context = mapper.toInternal(request);

        assertThat(context.getIdempotencyKey()).isEqualTo("transfer-001");
        assertThat(context.getVendorTransactionId()).isEqualTo("transfer-001");
        assertThat(context.getVendorCampaignCode()).isEqualTo("coupon-abc");
        assertThat(context.getVendorPayoutAmount()).isEqualByComparingTo("50.00");
        assertThat(context.getVendorPlayerUsername()).isEqualTo("player01");
        assertThat(context.getVendorCurrency()).isEqualTo("USD");
        assertThat(context.getToken()).isEqualTo("token-xyz");
        assertThat(context.getVendorSessionToken()).isEqualTo("token-xyz");
        assertThat(context.getVendorGameCode()).isEqualTo("game-001");
        assertThat(context.getPromoType()).isEqualTo(PromoType.FREE_ROUND);
        assertThat(context.getVendorTransactionTime()).isNotNull();
    }
}
