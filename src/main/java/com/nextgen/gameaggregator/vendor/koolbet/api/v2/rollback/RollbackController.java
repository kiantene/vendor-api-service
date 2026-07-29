package com.nextgen.gameaggregator.vendor.koolbet.api.v2.rollback;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.AbstractBetRollbackController;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackConfig;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.WalletRollbackServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackType;
import com.nextgen.gameaggregator.vendor.koolbet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.koolbet.response.CommonResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class RollbackController extends AbstractBetRollbackController<RollbackRequest, CommonResponse> {
    public RollbackController(RollbackRequestMapper requestMapper,
                              RollbackResponseMapper responseMapper,
                              WalletRollbackServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = EndPoints.CANCEL_BET + "/v2")
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME + "Rollback")
    public ResponseEntity<CommonResponse> rollback(@Valid @RequestBody RollbackRequest request) {
        return ResponseEntity.ok(processRequest(request, (context, resp) -> enrichResponse(resp, request)));
    }

    @Override
    public void configure(BetRollbackConfig config, RollbackRequest request) {
        // allowRollbackForSettledBet(false): an already-settled bet must not be cancellable.
        // RollbackPolicy then raises BetAlreadySettledException -> KoolbetExceptionMapper maps it
        // to ALREADY_ACCEPTED_AND_CANNOT_BE_CANCELED (code 6), which is terminal (HTTP 200,
        // vendorWillRetry=false), so Koolbet does not retry. Matches v1 and SessionRollbackController.
        config.rollbackType(RollbackType.BY_BET).allowRollbackForSettledBet(false).validateSessionToken(true);
    }

    private void enrichResponse(CommonResponse response, RollbackRequest request) {
        response.setCurrency(request.getCurrency());
    }
}
