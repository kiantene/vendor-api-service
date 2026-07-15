package com.nextgen.gameaggregator.vendor.evolutionv2.api.promo;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.promo.payout.AbstractPromoPayoutController;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutConfig;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutService;
import com.nextgen.gameaggregator.vendor.evolution.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.evolutionv2.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Evolution v2 promo-payout integration.
 */
@RestController
@RequestMapping(path = EndPoints.PATH)
public class PromoPayoutAction extends AbstractPromoPayoutController<PromoPayoutRequestDto, ResponseVo> {

    public PromoPayoutAction(PromoPayoutRequestMapper requestMapper,
                             PromoPayoutResponseMapper responseMapper,
                             PromoPayoutService promoPayoutService) {
        super(requestMapper, responseMapper, promoPayoutService);
    }

    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    @PostMapping(path = EndPoints.PROMO_PAYOUT)
    public ResponseEntity<ResponseVo> promoPayout(@Valid @RequestBody PromoPayoutRequestDto request) {
        return ResponseEntity.ok(processRequest(request));
    }

    @Override
    protected void configure(PromoPayoutConfig config, PromoPayoutRequestDto request) {
        config.playerUuidCampaignLookup(true);
    }
}
