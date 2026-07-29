package com.nextgen.gameaggregator.vendor.groove.api.freeround;

import com.nextgen.gameaggregator.core.engine.promo.campaign.CampaignResolveStrategy;
import com.nextgen.gameaggregator.core.engine.promo.payout.AbstractPromoPayoutController;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutConfig;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutService;
import com.nextgen.gameaggregator.vendor.groove.api.result.BetResultRequest;
import com.nextgen.gameaggregator.vendor.groove.api.result.BetResultResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class FreeRoundPayoutService extends AbstractPromoPayoutController<BetResultRequest, BetResultResponse> {

    public FreeRoundPayoutService(FreeRoundPayoutRequestMapper requestMapper,
                                  FreeRoundPayoutResponseMapper responseMapper,
                                  PromoPayoutService promoPayoutService) {
        super(requestMapper, responseMapper, promoPayoutService);
    }

    public ResponseEntity<BetResultResponse> freeRound(BetResultRequest request) {
        return ResponseEntity.ok(processRequest(request, (context, resp) -> enrichResponse(resp, request)));
    }

    private void enrichResponse(BetResultResponse response, BetResultRequest request) {
        response.setApiversion(request.getApiversion());
    }

    @Override
    protected void configure(PromoPayoutConfig config, BetResultRequest request) {
        config.campaignResolveStrategy(CampaignResolveStrategy.USERNAME_AND_BONUS_ID);
    }
}
