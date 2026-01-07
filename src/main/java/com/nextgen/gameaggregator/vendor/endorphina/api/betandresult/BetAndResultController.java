package com.nextgen.gameaggregator.vendor.endorphina.api.betandresult;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.vendor.endorphina.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetAndResultController extends AbstractBetResultController<BetAndResultRequest, BetAndResultResponse> {

    public BetAndResultController(BetAndResultRequestMapper requestMapper,
                                  BetAndResultResponseMapper responseMapper,
                                  WalletBetResultServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = EndPoints.BETANDRESULT, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<BetAndResultResponse> result(@Valid BetAndResultRequest request) {

        return ResponseEntity.ok(processRequest(request,
                (context, resp) -> enrichResponse(resp, request)));
    }

    @Override
    public void configure(BetResultConfig config, BetAndResultRequest request) {
        config.betAndResult(true)
                .returnSuccessOnDuplicate(true)
                .allowResultBeforeBet(true)
                .setSettleType(SettleType.BET);
    }

    private void enrichResponse(BetAndResultResponse response, BetAndResultRequest request) {
        response.setTransactionId(request.getId());
    }
}