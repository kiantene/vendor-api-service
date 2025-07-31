package com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashout;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatorstudio.service.VendorService;
import com.nextgen.gameaggregator.vendor.aviatorstudio.validator.AviatorStudioSignatureValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = EndPoints.PATH)
@RequiredArgsConstructor
public class CashOutController {
    private final WalletBetService walletService;
    private final CashOutRequestMapper requestMapper;
    private final CashOutResponseMapper responseMapper;

    @PostMapping(path = EndPoints.CASHOUT + "/v2")
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CashOutResponse> betAction(
            @RequestHeader(AviatorStudioSignatureValidator.HEADER_AUTHORIZATION) String jwt,
            @Valid @RequestBody CashOutRequest request) {

        BetContext context = requestMapper.toBetContext(request);
        enrich(context, jwt);
        PlayerBalanceData balanceData = walletService.process(context);

        return ResponseEntity.ok(responseMapper.toVendor(context, balanceData));
    }

    private void enrich(BetContext context, String jwt) {
        context.setVendorPlayerUsername(VendorService.jwtGetUserId(jwt));
    }
}
