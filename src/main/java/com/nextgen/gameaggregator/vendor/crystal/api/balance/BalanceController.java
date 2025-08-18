package com.nextgen.gameaggregator.vendor.crystal.api.balance;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateServiceWrapper;
import com.nextgen.gameaggregator.vendor.crystal.constant.EndPoints;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@RequiredArgsConstructor
public class BalanceController {
    private final BalanceRequestMapper requestMapper;
    private final BalanceResponseMapper responseMapper;
    private final AuthenticateServiceWrapper authenticateService;

    @PostMapping(path = EndPoints.BALANCE)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<BalanceResponse> getBalance(
            @Valid @RequestBody BalanceRequest request) {

        AuthenticateContext context = requestMapper.toInternal(request);
        PlayerBalanceData balanceData = authenticateService.process(context);
        return ResponseEntity.ok(responseMapper.toVendor(context, balanceData));
    }
}