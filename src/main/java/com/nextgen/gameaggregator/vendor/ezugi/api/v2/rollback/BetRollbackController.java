package com.nextgen.gameaggregator.vendor.ezugi.api.v2.rollback;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.*;
import com.nextgen.gameaggregator.vendor.ezugi.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetRollbackController extends AbstractBetRollbackController<BetRollbackRequest, BetRollbackResponse> {

    protected BetRollbackController(BetRollbackContextMapper<BetRollbackRequest> requestMapper,
                                    BetRollbackVendorResponseMapper<BetRollbackResponse> responseMapper,
                                    WalletRollbackServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = EndPoints.ROLLBACK)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<BetRollbackResponse> rollback(@Valid @RequestBody BetRollbackRequest request) {
        BetRollbackResponse response = processRequest(
                request,
                (context, resp) -> enrichResponse(resp, request)
        );

        return ResponseEntity.ok(response);
    }

    @Override
    public void configure(BetRollbackConfig config, BetRollbackRequest request) {
        config.rollbackType(RollbackType.BY_BET);
    }

    private void enrichResponse(BetRollbackResponse response, BetRollbackRequest request) {
        response.setOperatorId(request.getOperatorId());
    }
}
