package com.nextgen.gameaggregator.vendor.pragmaticplayv2.api.promo.tournament;

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
public class TournamentPayoutController extends AbstractPromoPayoutController<TournamentPayoutRequest, TournamentPayoutResponse> {

    protected TournamentPayoutController(TournamentPayoutRequestMapper requestMapper,
                                         TournamentPayoutResponseMapper responseMapper,
                                         PromoPayoutService promoPayoutService) {
        super(requestMapper, responseMapper, promoPayoutService);
    }

    @VendorExceptionHandler(className = Endpoints.CLASS_NAME)
    @PostMapping(path = Endpoints.PROMO)
    public ResponseEntity<TournamentPayoutResponse> promo(@Valid @ModelAttribute TournamentPayoutRequest request) {

        return ResponseEntity.ok(processRequest(request));
    }
}
