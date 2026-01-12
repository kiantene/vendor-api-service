package com.nextgen.gameaggregator.vendor.spribe.api.v2.rollback;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.AbstractBetRollbackController;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackConfig;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.WalletRollbackServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackType;
import com.nextgen.gameaggregator.vendor.spribe.config.SpribeConfig;
import com.nextgen.gameaggregator.vendor.spribe.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.spribe.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = Endpoints.PATH + "/v2")
public class RollbackController extends AbstractBetRollbackController<RollbackRequest, SuccessResponse> {

    public RollbackController(RollbackRequestMapper requestMapper,
                              RollbackResponseMapper responseMapper,
                              WalletRollbackServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = Endpoints.ROLLBACK)
    @VendorExceptionHandler(className = SpribeConfig.CLASS_NAME)
    public ResponseEntity<SuccessResponse> rollback(@Valid @RequestBody RollbackRequest request) {
        return ResponseEntity.ok(processRequest(
                request,
                (context, betResponse) -> enrichResponse(request, betResponse, context)
        ));
    }

    @Override
    public void configure(BetRollbackConfig config, RollbackRequest request) {
        config.rollbackType(RollbackType.BY_BET);
    }

    private void enrichResponse(RollbackRequest request, SuccessResponse response, BetRollbackContext context) {
        // TODO: OperatorTxId should map to a wallet transaction Id, but this is not supported yet so map to traceId first
        response.getData().setOperatorTxId(context.getTraceId());
        response.getData().setProvider(request.getProvider());
        response.getData().setProviderTxId(request.getProviderTxId());

        BigDecimal newBalance = response.getData().getNewBalance();
        response.getData().setOldBalance(newBalance.subtract(request.getAmount()));
    }
}
