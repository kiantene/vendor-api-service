package com.nextgen.gameaggregator.vendor.cockfight6.api.result;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.result.*;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.vendor.cockfight6.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cockfight6.request.CommonRequest;
import com.nextgen.gameaggregator.vendor.cockfight6.response.CommonSuccessResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ResultService extends AbstractBetResultController<CommonRequest, CommonSuccessResponse> {
    protected ResultService(BetResultContextMapper<CommonRequest> requestMapper, BetResultVendorResponseMapper<CommonSuccessResponse> responseMapper, WalletBetResultServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CommonSuccessResponse> result(CommonRequest request) {
        return ResponseEntity.ok(processRequest(request));
    }

    @Override
    protected void configure(BetResultConfig config, CommonRequest request) {
        config.settleType(SettleType.ROUND);
        config.allowResultWhenRoundHasEnded(false);
    }
}
