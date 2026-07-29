package com.nextgen.gameaggregator.vendor.wazdan.api.result;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.vendor.wazdan.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.wazdan.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetResultController extends AbstractBetResultController<BetResultRequest, SuccessResponse> {

    public BetResultController(BetResultRequestMapper requestMapper,
                               BetResultResponseMapper responseMapper,
                               WalletBetResultServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = EndPoints.RESULT)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<SuccessResponse> result(@Valid @RequestBody BetResultRequest request) {
        return ResponseEntity.ok(processRequest(request));
    }

    @Override
    public void configure(BetResultConfig config, BetResultRequest request) {
        config.betAndResult(false).setSettleType(SettleType.ROUND);
        config.allowResultWhenRoundHasEnded(false);
    }
}
