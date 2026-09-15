package com.nextgen.gameaggregator.vendor.esoterica.api.endround;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.balance.AbstractBalanceController;
import com.nextgen.gameaggregator.core.engine.wallet.balance.WalletBalanceServiceWrapper;
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
public class EndRoundController extends AbstractBalanceController<EndRoundRequest, CommonResponse> {

    protected EndRoundController(EndRoundRequestMapper requestMapper,
                                 EndRoundResponseMapper responseMapper,
                                 WalletBalanceServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(EndPoints.ENDROUND)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CommonResponse> endRound (@Valid @RequestBody EndRoundRequest request) {
        return ResponseEntity.ok(processRequest(request));
    }
}
