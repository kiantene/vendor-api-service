package com.nextgen.gameaggregator.vendor.digitain.api.rollback;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.AbstractBetRollbackController;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackConfig;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.WalletRollbackServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackType;
import com.nextgen.gameaggregator.vendor.digitain.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class RollbackController extends AbstractBetRollbackController<RollbackRequest, RollbackResponse> {
    public RollbackController(RollbackRequestMapper requestMapper,
                              RollbackResponseMapper responseMapper,
                              WalletRollbackServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = EndPoints.ROLLBACK)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<RollbackResponse> rollback(@Valid @RequestBody RollbackRequest request,
                                                     @RequestHeader(value = "SecretKey", required = true) String authorization) {

        RollbackResponse response = processRequest(request);
        return ResponseEntity.ok()
                .header("SecretKey", authorization)
                .body(response);
    }

    @Override
    public void configure(BetRollbackConfig config, RollbackRequest request) {
        config.rollbackType(
                request.getRefrnd().equals(true)
                        ? RollbackType.BY_ROUND
                        : RollbackType.BY_BET);
    }
}
