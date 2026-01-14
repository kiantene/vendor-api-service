package com.nextgen.gameaggregator.vendor.lucky365.api.result;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.vendor.lucky365.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.lucky365.constant.Mode;
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
public class BetResultController extends AbstractBetResultController<BetResultRequest, BetResultResponse> {
    private final MultiBetResultService multiBetResultService;

    public BetResultController(BetResultRequestMapper requestMapper,
                               BetResultResponseMapper responseMapper,
                               WalletBetResultServiceWrapper walletService,
                               MultiBetResultService multiBetResultService) {
        super(requestMapper, responseMapper, walletService);
        this.multiBetResultService = multiBetResultService;
    }

    @PostMapping(path = EndPoints.SETTLE)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<List<BetResultResponse>> result(@RequestBody List<@Valid BetResultRequest> request) {
        if (request == null) {
            throw new IllegalArgumentException("Request list is empty");
        }
        BetResultResponse response;
        //if betResultlist more than 1 will process error, this is to handle multiple betResult
        if (request.size() > 1) {
            response = multiBetResultService.process(request.get(1)); //dummy request
        } else {
            response = processRequest(request.get(0));
        }
        return ResponseEntity.ok(List.of(response));
    }

    @Override
    public void configure(BetResultConfig config, BetResultRequest request) {
        Mode mode = Mode.fromCode(request.getMode());
        config.betAndResult(mode == Mode.BET_AND_RESULT)
                .setSettleType(SettleType.BET);
    }
}