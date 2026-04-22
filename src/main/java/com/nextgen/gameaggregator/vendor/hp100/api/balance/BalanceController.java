package com.nextgen.gameaggregator.vendor.hp100.api.balance;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.balance.AbstractBalanceController;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContextMapper;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceVendorResponseMapper;
import com.nextgen.gameaggregator.core.engine.wallet.balance.WalletBalanceService;
import com.nextgen.gameaggregator.vendor.hp100.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.hp100.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class BalanceController extends AbstractBalanceController<BalanceRequest, SuccessResponse> {
    protected BalanceController(BalanceContextMapper<BalanceRequest> requestMapper,
                                BalanceVendorResponseMapper<SuccessResponse> responseMapper,
                                WalletBalanceService walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = Endpoints.BALANCE)
    @VendorExceptionHandler(className = Endpoints.CLASS_NAME)
    public ResponseEntity<SuccessResponse> balance(@Valid @RequestBody BalanceRequest request) {
        SuccessResponse response = processRequest(request);
        return ResponseEntity.ok(response);
    }
}
