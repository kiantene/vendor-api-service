package com.nextgen.gameaggregator.vendor.vplus.api.bet;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.bet.AbstractBetController;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetConfig;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService;
import com.nextgen.gameaggregator.vendor.vplus.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.vplus.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetController extends AbstractBetController<BetRequest, SuccessResponse> {
    protected BetController(BetRequestMapper requestMapper,
                            BetResponseMapper responseMapper,
                            WalletBetService walletBetService) {
        super(requestMapper, responseMapper, walletBetService);
    }

    @PostMapping(path = EndPoints.BET)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<SuccessResponse> bet(@Valid @RequestBody BetRequest request) {

        SuccessResponse response = processRequest(request);

        return ResponseEntity.ok(response);
    }

    @Override
    public void configure(BetConfig config, BetRequest request) {
        config.allowMultipleBet(false);
    }
}
