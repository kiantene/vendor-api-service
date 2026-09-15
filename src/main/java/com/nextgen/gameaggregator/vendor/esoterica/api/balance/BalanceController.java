package com.nextgen.gameaggregator.vendor.esoterica.api.balance;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.balance.AbstractBalanceController;
import com.nextgen.gameaggregator.core.engine.wallet.balance.WalletBalanceService;
import com.nextgen.gameaggregator.vendor.esoterica.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.esoterica.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BalanceController extends AbstractBalanceController<BalanceRequest, CommonResponse> {
    protected BalanceController(BalanceRequestMapper requestMapper,
                                BalanceResponseMapper responseMapper,
                                WalletBalanceService walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(EndPoints.BALANCE)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CommonResponse> balance(
            @Valid @RequestBody BalanceRequest request) {

        return ResponseEntity.ok(processRequest(request));
    }
}
