package com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = EndPoints.PATH)
@RequiredArgsConstructor
public class CashInController {
    private final CashInRequestMapper requestMapper;
    private final CashInResponseMapper responseMapper;
    private final WalletBetResultServiceWrapper walletService;

    @PostMapping(path = EndPoints.CASHIN + "/v2")
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CashInResponse> settleAction(
            @Valid @RequestBody CashInRequest request,
            @RequestAttribute("token") String token,
            @RequestAttribute("username") String username) {

        BetResultContext context = requestMapper.toBetResultContext(request);
        enrich(context, token, username);
        PlayerBalanceData balanceData = walletService
                .initialise(context)
                .isBetTxn(false)
                .process();
        return ResponseEntity.ok(responseMapper.toVendor(context, balanceData));
    }

    private void enrich(BetResultContext context, String token, String username) {
        context.setToken(token);
        context.setVendorPlayerUsername(username);
    }
}
