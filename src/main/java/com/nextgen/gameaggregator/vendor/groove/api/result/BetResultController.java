package com.nextgen.gameaggregator.vendor.groove.api.result;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.vendor.groove.api.freeround.FreeRoundPayoutService;
import com.nextgen.gameaggregator.vendor.groove.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetResultController extends AbstractBetResultController<BetResultRequest, BetResultResponse> {
    private final FreeRoundPayoutService freeRoundPayoutService;

    protected BetResultController(BetResultRequestMapper requestMapper,
                                  BetResultResponseMapper responseMapper,
                                  WalletBetResultServiceWrapper walletBetResultService,
                                  FreeRoundPayoutService freeRoundPayoutService) {
        super(requestMapper, responseMapper, walletBetResultService);
        this.freeRoundPayoutService = freeRoundPayoutService;
    }

    @GetMapping(params = EndPoints.RESULT)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<BetResultResponse> result(@Valid @ModelAttribute BetResultRequest request) {
        if (request.getFrbid() != null && !request.getFrbid().isBlank()) {
            return freeRoundPayoutService.freeRound(request);
        }

        return ResponseEntity.ok(processRequest(request, (context, resp) -> enrichResponse(resp, request)));
    }

    @Override
    public void configure(BetResultConfig config, BetResultRequest request) {
        config.betAndResult(false).setSettleType(SettleType.ROUND);
        config.allowResultWhenRoundHasEnded(false);
    }

    private void enrichResponse(BetResultResponse response, BetResultRequest request) {
        response.setApiversion(request.getApiversion());
    }
}
