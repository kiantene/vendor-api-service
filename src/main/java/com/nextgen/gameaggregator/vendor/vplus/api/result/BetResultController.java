package com.nextgen.gameaggregator.vendor.vplus.api.result;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.vendor.vplus.api.rollback.RollbackService;
import com.nextgen.gameaggregator.vendor.vplus.constant.BetResultType;
import com.nextgen.gameaggregator.vendor.vplus.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.vplus.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetResultController extends AbstractBetResultController<BetResultRequest, SuccessResponse> {
    private final RollbackService rollbackService;

    public BetResultController(BetResultRequestMapper requestMapper,
                               BetResultResponseMapper responseMapper,
                               WalletBetResultServiceWrapper walletService,
                               RollbackService rollbackService) {
        super(requestMapper, responseMapper, walletService);
        this.rollbackService = rollbackService;
    }

    @PostMapping(path = EndPoints.SETTLEMENT)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<SuccessResponse> result(@Valid @RequestBody BetResultRequest request) {

        SuccessResponse response = BetResultType.requiresSettlement(request.getType())
                ? processRequest(request)
                : rollbackService.doRollback(request);

        return ResponseEntity.ok(response);
    }

    @Override
    public void configure(BetResultConfig config, BetResultRequest request) {
        config.betAndResult(false)
                .settleType(SettleType.ROUND)
                .allowResultWhenRoundHasEnded(false);
    }
}
