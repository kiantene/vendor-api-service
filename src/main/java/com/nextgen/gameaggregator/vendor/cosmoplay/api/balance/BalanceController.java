package com.nextgen.gameaggregator.vendor.cosmoplay.api.balance;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AbstractAuthenticateController;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateService;
import com.nextgen.gameaggregator.vendor.cosmoplay.config.CosmoPlayVendorConfig;
import com.nextgen.gameaggregator.vendor.cosmoplay.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BalanceController extends AbstractAuthenticateController <BalanceRequest, BalanceResponse>{
    @Autowired
    protected BalanceController(
            BalanceRequestMapper requestMapper,
            BalanceResponseMapper responseMapper,
            AuthenticateService authenticateService
    ) {
        super(requestMapper, responseMapper, authenticateService);
    }

    @PostMapping(path = EndPoints.BALANCE)
    @VendorExceptionHandler(className = CosmoPlayVendorConfig.CLASS_NAME)
    public ResponseEntity<BalanceResponse> handle(
            @Valid @RequestBody
            BalanceRequest request
    ) {
        return ResponseEntity.ok(
                this.processRequest(request)
        );
    }
}
