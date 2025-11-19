package com.nextgen.gameaggregator.vendor.pragmaticplayv2.api.promo.freeround;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.promo.payout.AbstractPromoPayoutController;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutService;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class FreeRoundPayoutController extends AbstractPromoPayoutController<FreeRoundPayoutRequest, FreeRoundPayoutResponse> {

    protected FreeRoundPayoutController(FreeRoundPayoutRequestMapper requestMapper,
                                        FreeRoundPayoutResponseMapper responseMapper,
                                        PromoPayoutService promoPayoutService) {
        super(requestMapper, responseMapper, promoPayoutService);
    }

    @VendorExceptionHandler(className = Endpoints.CLASS_NAME)
    @PostMapping(path = Endpoints.BONUS)
    public ResponseEntity<FreeRoundPayoutResponse> bonus(@Valid @ModelAttribute FreeRoundPayoutRequest request) {

        return ResponseEntity.ok(processRequest(request));
    }
}
