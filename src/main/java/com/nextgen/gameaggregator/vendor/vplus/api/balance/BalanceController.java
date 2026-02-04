package com.nextgen.gameaggregator.vendor.vplus.api.balance;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.balance.AbstractBalanceController;
import com.nextgen.gameaggregator.core.engine.wallet.balance.WalletBalanceService;
import com.nextgen.gameaggregator.vendor.vplus.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.vplus.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BalanceController extends AbstractBalanceController<BalanceRequest, SuccessResponse> {
    protected BalanceController(BalanceRequestMapper requestMapper,
                                BalanceResponseMapper responseMapper,
                                WalletBalanceService walletBalanceService) {
        super(requestMapper, responseMapper, walletBalanceService);
    }

    @PostMapping(path = EndPoints.BALANCE)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<SuccessResponse> balance(@Valid @RequestBody BalanceRequest request) {
        SuccessResponse response = processRequest(request);

        return ResponseEntity.ok(response);
    }
}
