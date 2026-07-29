package com.nextgen.gameaggregator.vendor.cockfight6.api.rollback;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.*;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackType;
import com.nextgen.gameaggregator.vendor.cockfight6.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cockfight6.request.CommonRequest;
import com.nextgen.gameaggregator.vendor.cockfight6.response.CommonSuccessResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class RollbackService extends AbstractBetRollbackController<CommonRequest, CommonSuccessResponse> {
    protected RollbackService(BetRollbackContextMapper<CommonRequest> requestMapper, BetRollbackVendorResponseMapper<CommonSuccessResponse> responseMapper, WalletRollbackServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @VendorExceptionHandler(className = EndPoints.CLASS_NAME + "rollback")
    public ResponseEntity<CommonSuccessResponse> rollback(CommonRequest request) {
        return ResponseEntity.ok(processRequest(request));
    }

    @Override
    protected void configure(BetRollbackConfig config, CommonRequest request) {
        if (request.getSettle() != null)
            config.rollbackType(RollbackType.BY_ROUND);
    }
}
