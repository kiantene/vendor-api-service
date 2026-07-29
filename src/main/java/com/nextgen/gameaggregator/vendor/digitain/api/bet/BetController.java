package com.nextgen.gameaggregator.vendor.digitain.api.bet;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.bet.AbstractBetController;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService;
import com.nextgen.gameaggregator.vendor.digitain.constant.EndPoints;
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

    @PostMapping(path = EndPoints.BET)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<BetResponse> bet(@Valid @RequestBody BetRequest request, @RequestHeader(value = "SecretKey",
            required = true) String authorization) {

        BetResponse response = processRequest(request);
        return ResponseEntity.ok()
                .header("SecretKey",authorization)
                .body(response);
    }
}
