package com.nextgen.gameaggregator.vendor.cockfight6.api.bet;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.bet.AbstractBetController;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetVendorResponseMapper;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService;
import com.nextgen.gameaggregator.vendor.cockfight6.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cockfight6.request.CommonRequest;
import com.nextgen.gameaggregator.vendor.cockfight6.response.CommonSuccessResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class BetService extends AbstractBetController<CommonRequest, CommonSuccessResponse> {
    protected BetService(BetContextMapper<CommonRequest> requestMapper,
                         BetVendorResponseMapper<CommonSuccessResponse> responseMapper, WalletBetService walletService) {
        super(requestMapper, responseMapper, walletService);
    }


    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CommonSuccessResponse> bet(CommonRequest request) {
        return ResponseEntity.ok(processRequest(request));
    }
}
