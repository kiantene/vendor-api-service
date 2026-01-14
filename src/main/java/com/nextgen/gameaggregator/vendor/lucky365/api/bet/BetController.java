package com.nextgen.gameaggregator.vendor.lucky365.api.bet;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.bet.AbstractBetController;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetConfig;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService;
import com.nextgen.gameaggregator.vendor.lucky365.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping(path = EndPoints.PATH)
public class BetController extends AbstractBetController<BetRequest, BetResponse> {
    private final MultiBetService multiBetService;

    public BetController(BetRequestMapper requestMapper,
                         BetResponseMapper responseMapper,
                         WalletBetService walletService,
                         MultiBetService multiBetService) {
        super(requestMapper, responseMapper, walletService);
        this.multiBetService = multiBetService;
    }

    @PostMapping(path = EndPoints.BET)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<List<BetResponse>> bet(@RequestBody List<@Valid BetRequest> request) {
        if (request == null || request.isEmpty()) {
            throw new InternalServerException("Request list is empty");
        }
        BetResponse response;
        //if betlist more than 1 will process error, this is to handle multiple bet
        if (request.size() > 1) {
            response = multiBetService.process(request.get(1)); //dummy request
        } else {
            response = processRequest(request.get(0));
        }
        return ResponseEntity.ok(List.of(response));
    }

    @Override
    public void configure(BetConfig config, BetRequest request) {

        config.allowMultipleBet(false);
    }
}
