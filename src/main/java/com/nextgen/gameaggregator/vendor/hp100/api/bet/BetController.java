package com.nextgen.gameaggregator.vendor.hp100.api.bet;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.bet.AbstractBetController;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService;
import com.nextgen.gameaggregator.vendor.hp100.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.hp100.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class BetController extends AbstractBetController<BetRequest, SuccessResponse> {
    protected BetController(BetRequestMapper requestMapper,
                            BetResponseMapper responseMapper,
                            WalletBetService walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = Endpoints.BET)
    @VendorExceptionHandler(className = Endpoints.CLASS_NAME)
    public ResponseEntity<SuccessResponse> bet(@Valid @RequestBody BetRequest request) {
        return ResponseEntity.ok(processRequest(request, (context, resp) -> enrichResponse(resp, request)));
    }

    private void enrichResponse(SuccessResponse response, BetRequest request) {
        response.setSessionId(request.getSessionId());
        response.setTxId(request.getTxId());
    }
}
