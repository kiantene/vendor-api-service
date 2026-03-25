package com.nextgen.gameaggregator.vendor.cosmoplay.api.bet;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.bet.AbstractBetController;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetVendorResponseMapper;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService;
import com.nextgen.gameaggregator.vendor.cosmoplay.config.CosmoPlayVendorConfig;
import com.nextgen.gameaggregator.vendor.cosmoplay.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetController extends AbstractBetController<BetRequest, BetResponse> {
    protected BetController(
            BetContextMapper<BetRequest> requestMapper,
            BetVendorResponseMapper<BetResponse> responseMapper,
            WalletBetService walletService
    ) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = EndPoints.BETS)
    @VendorExceptionHandler(className = CosmoPlayVendorConfig.CLASS_NAME)
    public ResponseEntity<BetResponse> handle(
            @Valid
            @RequestBody
            BetRequest request
    ) {
        return ResponseEntity.ok(processRequest(request));
    }
}
