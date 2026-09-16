package com.nextgen.gameaggregator.vendor.casinogate.api.bet;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.bet.AbstractBetController;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetConfig;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService;
import com.nextgen.gameaggregator.vendor.casinogate.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.casinogate.response.CommonResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class BetController extends AbstractBetController<BetRequest, CommonResponse> {
    public BetController(BetRequestMapper requestMapper,
                         BetResponseMapper responseMapper,
                         WalletBetService walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = Endpoints.BET)
    @VendorExceptionHandler(className = Endpoints.CLASS_NAME)
    public ResponseEntity<CommonResponse> bet(@Valid @RequestBody BetRequest request) {
        return ResponseEntity.ok(processRequest(request));
    }

    @Override
    protected void configure(BetConfig config, BetRequest request) {
        config.allowMultipleBet(false)
                .allowBetWhenRoundHasEnded(false);
    }
}
