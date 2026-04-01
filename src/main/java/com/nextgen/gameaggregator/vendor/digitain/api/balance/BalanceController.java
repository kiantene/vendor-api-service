package com.nextgen.gameaggregator.vendor.digitain.api.balance;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.balance.AbstractBalanceController;
import com.nextgen.gameaggregator.core.engine.wallet.balance.WalletBalanceService;
import com.nextgen.gameaggregator.vendor.digitain.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BalanceController extends AbstractBalanceController<BalanceRequest, BalanceResponse> {

    protected BalanceController(BalanceRequestMapper requestMapper,
                                BalanceResponseMapper responseMapper,
                                WalletBalanceService walletBalanceService) {
        super(requestMapper, responseMapper, walletBalanceService);
    }

    @PostMapping(path = EndPoints.BALANCE)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<BalanceResponse> getBalance(
            @Valid @RequestBody BalanceRequest request,@RequestHeader(value = "SecretKey",
            required = true) String authorization) {

        BalanceResponse response = processRequest(request);
        return ResponseEntity.ok()
                .header("SecretKey",authorization)
                .body(response);
    }
}