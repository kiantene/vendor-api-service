package com.nextgen.gameaggregator.vendor.facai.api.promo;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.promo.payout.AbstractPromoPayoutController;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutConfig;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutService;
import com.nextgen.gameaggregator.vendor.facai.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class PromoPayoutController extends AbstractPromoPayoutController<PromoPayoutRequest, PromoPayoutResponse> {

    public PromoPayoutController(PromoPayoutRequestMapper requestMapper,
                                 PromoPayoutResponseMapper responseMapper,
                                 PromoPayoutService promoPayoutService) {
        super(requestMapper, responseMapper, promoPayoutService);
    }

    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    @PostMapping(path = EndPoints.PROMO_PAYOUT)
    public ResponseEntity<PromoPayoutResponse> promo(@Valid @ModelAttribute PromoPayoutRequest request) {

        return ResponseEntity.ok(processRequest(request));
    }

    @Override
    public void configure(PromoPayoutConfig config, PromoPayoutRequest request) {
        config.batch(true);
    }
}
