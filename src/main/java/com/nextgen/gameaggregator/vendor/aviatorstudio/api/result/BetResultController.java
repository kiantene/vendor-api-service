package com.nextgen.gameaggregator.vendor.aviatorstudio.api.result;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.result.*;
import com.nextgen.gameaggregator.vendor.aviatorstudio.api.rollback.RollbackService;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.ReasonCode;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetResultController extends AbstractBetResultController<BetResultRequest, BetResultResponse> {
    private final RollbackService rollbackService;

    public BetResultController(BetResultRequestMapper requestMapper,
                               BetResultResponseMapper responseMapper,
                               WalletBetResultServiceWrapper walletBetResultService,
                               RollbackService rollbackService) {
        super(requestMapper, responseMapper, walletBetResultService);
        this.rollbackService = rollbackService;
    }

    @PostMapping(path = EndPoints.CASHIN)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<BetResultResponse> result(
            @Valid @RequestBody BetResultRequest request,
            @RequestAttribute("token") String token,
            @RequestAttribute("username") String username) {

        BetResultResponse response = isSettle(request)
                ? doSettle(request, token, username)
                : rollbackService.doRollback(request, token, username);

        return ResponseEntity.ok(response);
    }

    @Override
    public void configure(BetResultConfig config) {
        config.betTxn(false)
                .setSettleType(SettleType.BET);
    }

    private boolean isSettle(BetResultRequest request) {
        return ReasonCode.isSettleReason(request.getReason());
    }

    private BetResultResponse doSettle(BetResultRequest request, String token, String username) {
        return processRequest(request, context -> enrichContext(context, token, username));
    }

    private void enrichContext(BetResultContext context, String token, String username) {
        context.setToken(token);
        context.setVendorPlayerUsername(username);
    }
}
