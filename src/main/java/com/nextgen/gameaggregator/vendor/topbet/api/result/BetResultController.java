package com.nextgen.gameaggregator.vendor.topbet.api.result;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.vendor.topbet.api.rollback.RollbackService;
import com.nextgen.gameaggregator.vendor.topbet.constant.Action;
import com.nextgen.gameaggregator.vendor.topbet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.topbet.response.SuccessResponse;
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

    @PostMapping(path = EndPoints.RESULT)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<SuccessResponse> result(@Valid @RequestBody BetResultRequest request) {
        if (request.getAction().equals(Action.GAME_BET_CANCEL)) {
            return ResponseEntity.ok(rollbackService.doRollback(request));
        }
        return ResponseEntity.ok(processRequest(request));
    }

    @Override
    public void configure(BetResultConfig config, BetResultRequest request) {
        config.betAndResult(false).settleType(SettleType.ROUND).allowResultWhenRoundHasEnded(false);
    }
}
