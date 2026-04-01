package com.nextgen.gameaggregator.vendor.digitain.api.betandresult;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.vendor.digitain.constant.EndPoints;
import jakarta.validation.Valid;
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

    @PostMapping(path = EndPoints.BETANDRESULT)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<BetAndResultResponse> result(@Valid @RequestBody BetAndResultRequest request,
            @RequestHeader(value = "SecretKey", required = true) String authorization) {

        BetAndResultResponse response = processRequest(request);
        return ResponseEntity.ok()
                .header("SecretKey", authorization)
                .body(response);
    }

    @Override
    public void configure(BetResultConfig config, BetAndResultRequest request) {
        config.betAndResult(true)
                .allowResultBeforeBet(true)
                .setSettleType(SettleType.ROUND);
    }
}