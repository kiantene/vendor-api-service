package com.nextgen.gameaggregator.vendor.aviatorstudio.api.bet;

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
public class BetController {
    private final WalletBetService walletService;
    private final BetRequestMapper requestMapper;
    private final BetResponseMapper responseMapper;

    @PostMapping(path = EndPoints.CASHOUT)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<BetResponse> doBet(
            @Valid @RequestBody BetRequest request,
            @RequestAttribute("token") String token,
            @RequestAttribute("username") String username) {

        BetContext context = requestMapper.toBetContext(request);
        enrich(context, token, username);
        PlayerBalanceData balanceData = walletService.process(context);
        return ResponseEntity.ok(responseMapper.toVendor(context, balanceData));
    }

    private void enrich(BetContext context, String token, String username) {
        context.setToken(token);
        context.setVendorPlayerUsername(username);
    }
}
