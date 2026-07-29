package com.nextgen.gameaggregator.vendor.koolbet.api.v2.balance;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.balance.AbstractBalanceController;
import com.nextgen.gameaggregator.core.engine.wallet.balance.WalletBalanceService;
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
public class BalanceController extends AbstractBalanceController<BalanceRequest, CommonResponse> {
    protected BalanceController(BalanceRequestMapper requestMapper,
                                BalanceResponseMapper responseMapper,
                                WalletBalanceService walletBalanceService) {
        super(requestMapper, responseMapper, walletBalanceService);
    }

    @PostMapping(path = EndPoints.BALANCE + "/v2")
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CommonResponse> balance(
            @Valid @RequestBody BalanceRequest request) {
        return ResponseEntity.ok(processRequest(request));
    }
}
