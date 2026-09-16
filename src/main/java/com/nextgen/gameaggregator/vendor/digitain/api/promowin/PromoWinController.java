package com.nextgen.gameaggregator.vendor.digitain.api.promowin;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.promo.payout.AbstractPromoPayoutController;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutService;
import com.nextgen.gameaggregator.vendor.digitain.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = EndPoints.PATH)
/**
 * Digitain promo-win payout.
 *
 * <p>Moved off {@code AbstractBetResultController} in ONEAPI-372. These wins have no bet behind them —
 * Digitain's spec says so outright — but the bet-result engine booked each one into bet history as a
 * zero-stake win, so vendor-side GGR exceeded operator-side and reconciliation drifted. They now go to the
 * operator via {@code /v1/promo/payout} and land in {@code promo_payout_history} with a {@code promo_type}.
 *
 * <p>No {@code configure()} override: Digitain resolves no campaign, so there is no strategy to select.
 */
public class PromoWinController extends AbstractPromoPayoutController<PromoWinRequest, PromoWinResponse> {

    public PromoWinController(PromoWinRequestMapper requestMapper,
                              PromoWinResponseMapper responseMapper,
                              PromoPayoutService promoPayoutService) {
        super(requestMapper, responseMapper, promoPayoutService);
    }

    @PostMapping(path = EndPoints.PROMOWIN)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME+"PromoWin")
    public ResponseEntity<PromoWinResponse> result(@Valid @RequestBody PromoWinRequest request,
                                                   @RequestHeader(value = "SecretKey", required = true) String authorization) {

        PromoWinResponse response = processRequest(request);
        return ResponseEntity.ok()
                .header("SecretKey", authorization)
                .body(response);
    }
}