package com.nextgen.gameaggregator.vendor.endorphina.api.result;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;

import com.nextgen.gameaggregator.vendor.endorphina.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetResultController extends AbstractBetResultController<BetResultRequest, BetResultResponse> {

    public BetResultController(BetResultRequestMapper requestMapper,
                               BetResultResponseMapper responseMapper,
                               WalletBetResultServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = EndPoints.SETTLE, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<BetResultResponse> result(@Valid BetResultRequest request) {

        return ResponseEntity.ok(processRequest(request));
    }

    @Override
    public void configure(BetResultConfig config, BetResultRequest request) {
        config.betAndResult(false)
                .returnSuccessOnDuplicate(true)
                .setSettleType(SettleType.BET);
    }
}