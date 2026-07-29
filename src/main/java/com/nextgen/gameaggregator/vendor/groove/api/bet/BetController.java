package com.nextgen.gameaggregator.vendor.groove.api.bet;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.bet.AbstractBetController;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetConfig;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService;
import com.nextgen.gameaggregator.vendor.groove.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetController extends AbstractBetController<BetRequest, BetResponse> {
    protected BetController(BetRequestMapper requestMapper,
                            BetResponseMapper responseMapper,
                            WalletBetService walletBetService) {
        super(requestMapper, responseMapper, walletBetService);
    }

    @GetMapping(params = EndPoints.BET)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<BetResponse> bet(@Valid @ModelAttribute BetRequest request) {
        return ResponseEntity.ok(processRequest(request, (context, resp) -> enrichResponse(resp, request)));
    }

    private void enrichResponse(BetResponse response, BetRequest request) {
        response.setApiversion(request.getApiversion());
    }

    @Override
    protected void configure(BetConfig config, BetRequest request) {
        config.allowBetWhenRoundHasEnded(false);
    }
}