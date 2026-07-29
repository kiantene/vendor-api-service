package com.nextgen.gameaggregator.vendor.koolbet.api.v2.sessionrollback;

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
public class SessionRollbackController extends AbstractBetRollbackController<SessionRollbackRequest, CommonResponse> {
    public SessionRollbackController(SessionRollbackRequestMapper requestMapper,
                                     SessionRollbackResponseMapper responseMapper,
                                     WalletRollbackServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = EndPoints.CANCEL_SESSION_BET + "/v2")
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME + "Rollback")
    public ResponseEntity<CommonResponse> rollback(@Valid @RequestBody SessionRollbackRequest request) {
        return ResponseEntity.ok(processRequest(request, (context, resp) -> enrichResponse(resp, request)));
    }

    @Override
    public void configure(BetRollbackConfig config, SessionRollbackRequest request) {
        config.validateSessionToken(true).rollbackType(RollbackType.BY_BET);
    }

    private void enrichResponse(CommonResponse response, SessionRollbackRequest request) {
        response.setCurrency(request.getCurrency());
    }
}
