package com.nextgen.gameaggregator.vendor.lucky365.api.rollback;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.AbstractBetRollbackController;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackConfig;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.WalletRollbackServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackType;
import com.nextgen.gameaggregator.vendor.lucky365.api.result.BetResultRequest;
import com.nextgen.gameaggregator.vendor.lucky365.api.result.BetResultResponse;
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
public class RollbackController extends AbstractBetRollbackController<RollbackRequest, RollbackResponse> {
    private final MultiRollbackService multiRollbackService;
    public RollbackController(RollbackRequestMapper requestMapper,
                              RollbackResponseMapper responseMapper,
                              WalletRollbackServiceWrapper walletService, MultiRollbackService multiRollbackService) {
        super(requestMapper, responseMapper, walletService);
        this.multiRollbackService = multiRollbackService;
    }
//close this endpoint cause vendor did not provide service
    @PostMapping(path = EndPoints.ROLLBACK)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<List<RollbackResponse>> rollback(@RequestBody List<@Valid RollbackRequest> request) {
        if (request == null) {
            throw new IllegalArgumentException("Request list is empty");
        }
        RollbackResponse response;
        //if rollbacklist more than 1 will process error, this is to handle multiple rollback
        if (request.size() > 1) {
            response = multiRollbackService.process(request.get(1)); //dummy request
        } else {
            response = processRequest(request.get(0));
        }
        return ResponseEntity.ok(List.of(response));
    }

    @Override
    public void configure(BetRollbackConfig config, RollbackRequest request) {

        config.rollbackType(RollbackType.BY_BET);
    }
}
