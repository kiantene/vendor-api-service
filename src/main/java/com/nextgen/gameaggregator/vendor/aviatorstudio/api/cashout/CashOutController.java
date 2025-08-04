package com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashout;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
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
            @Valid @RequestBody CashOutRequest request,
            @RequestAttribute("username") String username) {

        BetContext context = requestMapper.toBetContext(request);
        enrich(context, username);
        PlayerBalanceData balanceData = walletService.process(context);
        return ResponseEntity.ok(responseMapper.toVendor(context, balanceData));
    }

    private void enrich(BetContext context, String username) {
        context.setVendorPlayerUsername(username);
    }
}
