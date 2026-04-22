package com.nextgen.gameaggregator.vendor.hp100.api.rollback;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.*;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackType;
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
public class RollbackController extends AbstractBetRollbackController<RollbackRequest, SuccessResponse> {
    protected RollbackController(BetRollbackContextMapper<RollbackRequest> requestMapper, BetRollbackVendorResponseMapper<SuccessResponse> responseMapper, WalletRollbackServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = Endpoints.ROLLBACK)
    @VendorExceptionHandler(className = Endpoints.CLASS_NAME + "rollback")
    public ResponseEntity<SuccessResponse> rollback(@Valid @RequestBody RollbackRequest request) {
        return ResponseEntity.ok(processRequest(request, (context, resp) -> enrichResponse(resp, request)));
    }


    @Override
    public void configure(BetRollbackConfig config, RollbackRequest request) {
        config.allowRollbackForSettledBet(false);
        config.rollbackType(RollbackType.BY_BET);
    }

    private void enrichResponse(SuccessResponse response, RollbackRequest request) {
        response.setSessionId(request.getSessionId());
    }

}
