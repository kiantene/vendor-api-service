package com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatorstudio.service.VendorService;
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

    @PostMapping(path = EndPoints.CASHIN)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CashInResponse> settleAction(
            @Valid @RequestBody CashInRequest request,
            @RequestAttribute("username") String username) {

        BetResultContext context = requestMapper.toBetResultContext(request);
        enrich(context, username);
        PlayerBalanceData balanceData = walletService
                .initialise(context)
                .isBetTxn(false)
                .vendorService(vendorService)
                .process();
        return ResponseEntity.ok(responseMapper.toVendor(context, balanceData));
    }

    private void enrich(BetResultContext context, String username) {
        context.setVendorPlayerUsername(username);
    }
}
