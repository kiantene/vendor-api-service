package com.nextgen.gameaggregator.vendor.hp100.api.result;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.vendor.hp100.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.hp100.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class BetResultController extends AbstractBetResultController<BetResultRequest, SuccessResponse> {
    protected BetResultController(BetResultRequestMapper requestMapper,
                                  BetResultResponseMapper responseMapper,
                                  WalletBetResultServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = Endpoints.RESULT)
    @VendorExceptionHandler(className = Endpoints.CLASS_NAME + "settle")
    public ResponseEntity<SuccessResponse> result(@Valid @RequestBody BetResultRequest request) {
        return ResponseEntity.ok(processRequest(request, (context, resp) -> enrichResponse(resp, request)));
    }

    private void enrichResponse(SuccessResponse response, BetResultRequest request) {
        response.setSessionId(request.getSessionId());
    }
}
