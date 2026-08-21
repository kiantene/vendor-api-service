package com.nextgen.gameaggregator.vendor.dotconnections.api.freespin;

import com.nextgen.gameaggregator.core.engine.promo.campaign.CampaignResolveStrategy;
import com.nextgen.gameaggregator.core.engine.promo.payout.AbstractPromoPayoutController;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutConfig;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutService;
import com.nextgen.gameaggregator.vendor.dotconnections.vo.ResponseVo;
import org.springframework.stereotype.Component;

/**
 * Free-round payout for Dot Connections.
 *
 * <p>{@code freeSpinResult} is the only DCS callback that pays out the grants we provision — it carries
 * the {@code freespin_id} that {@code createFreeSpin} minted. The sibling {@code promoPayout} callback
 * is a separate channel keyed by {@code promotion_id} with no game or round context, and nothing we
 * provision produces one, so it is deliberately left on its original path.
 */
@Component
public class DotConnectionsFreeSpinResultHandler extends AbstractPromoPayoutController<FreeSpinResultDto, ResponseVo> {

    public DotConnectionsFreeSpinResultHandler(DotConnectionsFreeSpinResultRequestMapper requestMapper,
                                               DotConnectionsFreeSpinResultResponseMapper responseMapper,
                                               PromoPayoutService promoPayoutService) {
        super(requestMapper, responseMapper, promoPayoutService);
    }

    public ResponseVo process(FreeSpinResultDto request) {
        return processRequest(request);
    }

    @Override
    protected void configure(PromoPayoutConfig config, FreeSpinResultDto request) {
        config.campaignResolveStrategy(CampaignResolveStrategy.USERNAME_AND_BONUS_ID);
    }
}
