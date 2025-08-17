package com.nextgen.gameaggregator.vendor.aviatorstudio.api.bet;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.bet.AbstractBetController;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetController extends AbstractBetController<BetRequest, BetResponse> {
    public BetController(BetRequestMapper requestMapper,
                         BetResponseMapper responseMapper,
                         WalletBetService walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = EndPoints.CASHOUT)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<BetResponse> bet(
            @Valid @RequestBody BetRequest request,
            @RequestAttribute("token") String token,
            @RequestAttribute("username") String username) {

        BetResponse response = processRequest(
                request,
                context -> enrichContext(context, token, username)
        );
        return ResponseEntity.ok(response);
    }

    private void enrichContext(BetContext context, String token, String username) {
        context.setToken(token);
        context.setVendorPlayerUsername(username);
    }
}
