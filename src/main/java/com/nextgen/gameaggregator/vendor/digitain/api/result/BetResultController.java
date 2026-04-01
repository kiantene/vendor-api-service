package com.nextgen.gameaggregator.vendor.digitain.api.result;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.vendor.digitain.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetResultController extends AbstractBetResultController<BetResultRequest, BetResultResponse> {

    public BetResultController(BetResultRequestMapper requestMapper,
            BetResultResponseMapper responseMapper,
            WalletBetResultServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = EndPoints.RESULT)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<BetResultResponse> result(@Valid @RequestBody BetResultRequest request,
            @RequestHeader(value = "SecretKey", required = true) String authorization) {

        BetResultResponse response = processRequest(request);
        return ResponseEntity.ok()
                .header("SecretKey", authorization)
                .body(response);
    }

    @Override
    public void configure(BetResultConfig config, BetResultRequest request) {
        config.betAndResult(false)
                .setSettleType(SettleType.ROUND);
    }
}