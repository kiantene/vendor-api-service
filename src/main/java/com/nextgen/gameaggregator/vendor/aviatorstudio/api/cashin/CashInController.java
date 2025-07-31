package com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
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
public class CashInController {
    private final WalletBetResultServiceWrapper walletService;
    private final CashInRequestMapper requestMapper;
    private final CashInResponseMapper responseMapper;
    private final VendorService vendorService;

    @PostMapping(path = EndPoints.CASHIN + "/v2")
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CashInResponse> settleAction(
            @RequestHeader(AviatorStudioSignatureValidator.HEADER_AUTHORIZATION) String jwt,
            @Valid @RequestBody CashInRequest request) {

        BetResultContext context = requestMapper.toBetResultContext(request);
        enrich(context, jwt);
        PlayerBalanceData balanceData = walletService
                .initialise(context)
                .isBetTxn(false)
                .vendorService(vendorService)
                .process();

        return ResponseEntity.ok(responseMapper.toVendor(context, balanceData));
    }

    private void enrich(BetResultContext context, String jwt) {
        context.setVendorPlayerUsername(VendorService.jwtGetUserId(jwt));
    }
}
