package com.nextgen.gameaggregator.vendor.groove.api.freeround;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.enums.PromoType;
import com.nextgen.gameaggregator.vendor.groove.api.result.BetResultRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FreeRoundPayoutRequestMapperTest {

    private final FreeRoundPayoutRequestMapper mapper = new FreeRoundPayoutRequestMapper();

    @Test
    void toInternal_mapsGrooveFieldsForResolveByUsernameAndBonusId() {
        BetResultRequest request = new BetResultRequest();
        request.setTransactionid("txn-1");
        request.setAccountid("player-1");
        request.setFrbid("frb-1");
        request.setResult(new BigDecimal("1.50"));
        request.setGamesessionid("session_token");

        PromoPayoutContext ctx = mapper.toInternal(request);

        // frbid now lives in its own honest field, not smuggled through vendorCampaignCode
        assertThat(ctx.getVendorFreeRoundBonusId()).isEqualTo("frb-1");
        assertThat(ctx.getVendorCampaignCode()).isNull();
        // username is the other half of the USERNAME_AND_BONUS_ID lookup
        assertThat(ctx.getVendorPlayerUsername()).isEqualTo("player-1");
        assertThat(ctx.getPromoType()).isEqualTo(PromoType.FREE_ROUND);
        assertThat(ctx.getVendorTransactionId()).isEqualTo("txn-1");
        assertThat(ctx.getVendorPayoutAmount()).isEqualByComparingTo("1.50");
    }
}
