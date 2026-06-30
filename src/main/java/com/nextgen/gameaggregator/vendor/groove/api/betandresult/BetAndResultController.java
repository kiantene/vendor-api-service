package com.nextgen.gameaggregator.vendor.groove.api.betandresult;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.vendor.groove.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetAndResultController extends AbstractBetResultController<BetAndResultRequest, BetAndResultResponse> {

    public BetAndResultController(BetAndResultRequestMapper requestMapper,
                                  BetAndResultResponseMapper responseMapper,
                                  WalletBetResultServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @GetMapping(params = EndPoints.BETANDRESULT)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<BetAndResultResponse> result(@Valid @ModelAttribute BetAndResultRequest request) {
        return ResponseEntity.ok(processRequest(request, (context, resp) -> enrichResponse(resp, request)));
    }

    @Override
    public void configure(BetResultConfig config, BetAndResultRequest request) {
        config.betAndResult(true).setSettleType(SettleType.ROUND);
        config.allowResultWhenRoundHasEnded(false);
    }

    private void enrichResponse(BetAndResultResponse response, BetAndResultRequest request) {
        response.setApiversion(request.getApiversion());
    }
}