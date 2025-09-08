package com.nextgen.gameaggregator.vendor.crystal.api.rollback;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.AbstractBetRollbackController;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackConfig;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.RollbackType;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.WalletRollbackServiceWrapper;
import com.nextgen.gameaggregator.vendor.crystal.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class RollbackController extends AbstractBetRollbackController<RollbackRequest, RollbackResponse> {
    public RollbackController(RollbackRequestMapper requestMapper,
            RollbackResponseMapper responseMapper,
            WalletRollbackServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = EndPoints.REFUND)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<RollbackResponse> rollback(@Valid @RequestBody RollbackRequest request) {
        return ResponseEntity.ok(processRequest(request,(context, resp)-> enrichResponse(resp, request)));
    }

    @Override
    public void configure(BetRollbackConfig config, RollbackRequest request) {
        config.setRollbackType(RollbackType.BY_ROUND);
    }

    private void enrichResponse(RollbackResponse response, RollbackRequest request) {
        RollbackResponse.Data data = RollbackResponse.Data.builder()
                .actionId(request.getTransactionId())
                .build();
        response.setData(data);
    }
}
