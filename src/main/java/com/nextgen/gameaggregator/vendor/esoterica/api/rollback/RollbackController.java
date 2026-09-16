package com.nextgen.gameaggregator.vendor.esoterica.api.rollback;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.*;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackType;
import com.nextgen.gameaggregator.vendor.esoterica.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.esoterica.response.CommonResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class RollbackController extends AbstractBetRollbackController<RollbackRequest, CommonResponse> {

    protected RollbackController(RollbackRequestMapper requestMapper,
                                 RollbackResponseMapper responseMapper,
                                 WalletRollbackServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = EndPoints.REFUND)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CommonResponse> rollback(@Valid @RequestBody RollbackRequest request) {
        return ResponseEntity.ok(processRequest(request));
    }

    @Override
    protected void configure(BetRollbackConfig config, RollbackRequest request) {
        config.rollbackType(RollbackType.BY_BET).allowRollbackForSettledBet(true);
    }
}
