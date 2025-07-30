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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@RequiredArgsConstructor
public class CashOutController {
    private final WalletBetService walletBetService;
    private final CashOutRequestMapper requestMapper;
    private final CashOutResponseMapper responseMapper;

//    @PostMapping(path = EndPoints.CASHOUT)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CashOutResponse> betAction(
            @RequestHeader(AviatorStudioSignatureValidator.HEADER_AUTHORIZATION) String jwt,
            @Valid @RequestBody CashOutRequest request) {

        BetContext betContext = requestMapper.toBetContext(request);
        enrich(betContext, jwt);
        PlayerBalanceData balanceData = walletBetService.process(betContext);

        return ResponseEntity.ok(responseMapper.toVendor(betContext, balanceData));
    }

    private void enrich(BetContext context, String jwt) {
        context.setVendorPlayerUsername(VendorService.jwtGetUserId(jwt));
    }
}
