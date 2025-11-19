package com.nextgen.gameaggregator.vendor.pragmaticplayv2.api.promo.tournament;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutVendorResponseMapper;
import org.springframework.stereotype.Component;

@Component
public class TournamentPayoutResponseMapper implements PromoPayoutVendorResponseMapper<TournamentPayoutResponse> {

    @Override
    public TournamentPayoutResponse toVendor(PromoPayoutContext context, PlayerBalanceData balanceData) {
        return TournamentPayoutResponse.builder()
                .build();
    }
}
