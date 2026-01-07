package com.nextgen.gameaggregator.vendor.endorphina.api.bet;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.bet.AbstractBetController;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetConfig;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService;
import com.nextgen.gameaggregator.vendor.endorphina.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetController extends AbstractBetController<BetRequest, BetResponse> {
    public BetController(BetRequestMapper requestMapper,
                         BetResponseMapper responseMapper,
                         WalletBetService walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = EndPoints.BET, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<BetResponse> bet(@Valid BetRequest request) {
        return ResponseEntity.ok(processRequest(request,
                (context, resp) -> enrichResponse(resp, request)));
    }

    @Override
    public void configure(BetConfig config, BetRequest request) {

        config.returnSuccessOnDuplicate(true);
    }

    private void enrichResponse(BetResponse response, BetRequest request) {

        response.setTransactionId(request.getId());
    }

}
