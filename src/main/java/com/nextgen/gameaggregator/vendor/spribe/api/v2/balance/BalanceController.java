package com.nextgen.gameaggregator.vendor.spribe.api.v2.balance;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.balance.AbstractBalanceController;
import com.nextgen.gameaggregator.core.engine.wallet.balance.WalletBalanceService;
import com.nextgen.gameaggregator.vendor.spribe.config.SpribeConfig;
import com.nextgen.gameaggregator.vendor.spribe.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.spribe.response.BalanceResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class BalanceController extends AbstractBalanceController<BalanceRequest, BalanceResponse> {
    public BalanceController(BalanceRequestMapper requestMapper,
                             BalanceResponseMapper responseMapper,
                             WalletBalanceService walletBalanceService) {
        super(requestMapper, responseMapper, walletBalanceService);
    }

    @PostMapping(path = Endpoints.INFO)
    @VendorExceptionHandler(className = SpribeConfig.CLASS_NAME)
    public ResponseEntity<BalanceResponse> getBalance(@Valid @RequestBody BalanceRequest request) {
        return ResponseEntity.ok(processRequest(request));
    }
}
