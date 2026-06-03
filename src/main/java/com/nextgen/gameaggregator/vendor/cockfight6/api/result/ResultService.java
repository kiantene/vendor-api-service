package com.nextgen.gameaggregator.vendor.cockfight6.api.result;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.result.*;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.vendor.cockfight6.api.rollback.RollbackService;
import com.nextgen.gameaggregator.vendor.cockfight6.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cockfight6.request.CommonRequest;
import com.nextgen.gameaggregator.vendor.cockfight6.response.CommonSuccessResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ResultService extends AbstractBetResultController<CommonRequest, CommonSuccessResponse> {
    private final RollbackService rollbackService;

    protected ResultService(BetResultContextMapper<CommonRequest> requestMapper, BetResultVendorResponseMapper<CommonSuccessResponse> responseMapper, WalletBetResultServiceWrapper walletService, RollbackService rollbackService) {
        super(requestMapper, responseMapper, walletService);
        this.rollbackService = rollbackService;
    }

    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CommonSuccessResponse> result(CommonRequest request) {
        int status = Integer.parseInt(request.getSettle().getResult().split(":")[0]);

        if (status != 0) {
            return rollbackService.rollback(request);
        }
        return ResponseEntity.ok(processRequest(request));
    }

    @Override
    protected void configure(BetResultConfig config, CommonRequest request) {
        config.settleType(SettleType.ROUND);
        config.allowResultWhenRoundHasEnded(false);
    }
}
