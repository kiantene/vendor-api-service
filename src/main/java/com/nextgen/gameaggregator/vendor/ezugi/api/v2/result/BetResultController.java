package com.nextgen.gameaggregator.vendor.ezugi.api.v2.result;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.vendor.ezugi.constant.BetTypeID;
import com.nextgen.gameaggregator.vendor.ezugi.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ezugi.constant.ReturnReasons;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetResultController extends AbstractBetResultController<BetResultRequest, BetResultResponse> {
    private final BetRollbackService betRollbackService;

    public BetResultController(BetResultRequestMapper requestMapper,
                               BetResultResponseMapper responseMapper,
                               WalletBetResultServiceWrapper walletBetResultService,
                               BetRollbackService betRollbackService) {
        super(requestMapper, responseMapper, walletBetResultService);
        this.betRollbackService = betRollbackService;
    }

    @PostMapping(path = EndPoints.CREDIT)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<BetResultResponse> result(@Valid @RequestBody BetResultRequest request) {

        BetResultResponse response;
        if (isRollback(request)) {
            response = betRollbackService.rollback(request);
        } else {
            response = this.processRequest(
                    request,
                    (ctx, resp) -> enrichResponse(resp, request));
        }

        return ResponseEntity.ok(response);
    }

    @Override
    public void configure(BetResultConfig config, BetResultRequest request) {
        BetResultConfig.ProcessingMode mode = request.getGameDataString() != null
                ? BetResultConfig.ProcessingMode.BATCH
                : BetResultConfig.ProcessingMode.SINGLE;

        config.betAndResult(false)
                .settleType(SettleType.BET)
                .publishBetHistoryOnRoundEnded(request.isEndRound())
                .processingMode(mode);
    }

    private void enrichResponse(BetResultResponse response, BetResultRequest request) {
        response.setOperatorId(request.getOperatorId());
        response.setRoundId(request.getRoundId());
    }

    private boolean isSettle(BetResultRequest request) {
        return BetTypeID.VALID_CREDIT_BET_TYPE_ID.get(request.getBetTypeId()) != null;
    }

    private boolean isRollback(BetResultRequest request) {
        return request.getReturnReason() != null &&
                (request.getReturnReason() == ReturnReasons.CANCEL_BET ||
                        request.getReturnReason() == ReturnReasons.CANCELED_ROUND);
    }
}
