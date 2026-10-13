package com.nextgen.gameaggregator.vendor.megafair.api.rollback;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.AbstractBetRollbackController;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackConfig;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.WalletRollbackServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackType;
import com.nextgen.gameaggregator.vendor.megafair.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class RollbackController extends AbstractBetRollbackController<RollbackRequest, RollbackResponse> {

    public RollbackController(RollbackRequestMapper requestMapper,
                              RollbackResponseMapper responseMapper,
                              WalletRollbackServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = EndPoints.REFUND, produces = MediaType.APPLICATION_XML_VALUE)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<RollbackResponse> rollback(@Valid @ModelAttribute RollbackRequest request) {
        RollbackResponse response = processRequest(
                request,
                (context, resp) -> enrichResponse(resp, request)
        );

        return ResponseEntity.ok(response);
    }

    @Override
    public void configure(BetRollbackConfig config, RollbackRequest request) {
        config.rollbackType(RollbackType.BY_BET);
    }

    private void enrichResponse(RollbackResponse response, RollbackRequest request) {
        if (response.getRequest() == null) {
            response.setRequest(RollbackResponse.RequestData.builder().build());
        }

        RollbackResponse.RequestData requestData = response.getRequest();

        requestData.setUserId(request.getUserId());
        requestData.setCasinoTransactionId(request.getCasinoTransactionId());
        requestData.setGameSessionId(request.getGameSessionId());
        requestData.setHash(request.getHash());
    }
}
