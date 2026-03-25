package com.nextgen.gameaggregator.vendor.cosmoplay.api.result;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultVendorResponseMapper;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.vendor.cosmoplay.config.CosmoPlayVendorConfig;
import com.nextgen.gameaggregator.vendor.cosmoplay.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetResultController extends AbstractBetResultController<BetResultRequest, BetResultResponse> {

    protected BetResultController(
            BetResultContextMapper<BetResultRequest> requestMapper,
            BetResultVendorResponseMapper<BetResultResponse> responseMapper,
            WalletBetResultServiceWrapper walletService
    ) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = EndPoints.WIN_RESULT)
    @VendorExceptionHandler(className = CosmoPlayVendorConfig.CLASS_NAME)
    public ResponseEntity<BetResultResponse> handle(
            @Valid
            @RequestBody
            BetResultRequest request
    ) {
        return ResponseEntity.ok(processRequest(request));
    }

    @Override
    protected void configure(BetResultConfig config, BetResultRequest request) {
        config.returnSuccessOnDuplicate(true);
    }
}
