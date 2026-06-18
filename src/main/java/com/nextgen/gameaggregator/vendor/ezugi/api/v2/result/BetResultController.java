package com.nextgen.gameaggregator.vendor.ezugi.api.v2.result;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.data.kafka.betdetails.BetDetailEmitRequest;
import com.nextgen.gameaggregator.data.kafka.betdetails.EventKind;
import com.nextgen.gameaggregator.data.kafka.betdetails.RawBetDetailsProducer;
import com.nextgen.gameaggregator.vendor.ezugi.api.v2.RawBetDetailGaBetIdResolver;
import com.nextgen.gameaggregator.vendor.ezugi.constant.BetTypeID;
import com.nextgen.gameaggregator.vendor.ezugi.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ezugi.constant.ReturnReasons;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BetResultController extends AbstractBetResultController<BetResultRequest, BetResultResponse> {
    private final BetRollbackService betRollbackService;
    private final RawBetDetailsProducer rawBetDetailsProducer;

    public BetResultController(BetResultRequestMapper requestMapper,
                               BetResultResponseMapper responseMapper,
                               WalletBetResultServiceWrapper walletBetResultService,
                               BetRollbackService betRollbackService,
                               RawBetDetailsProducer rawBetDetailsProducer) {
        super(requestMapper, responseMapper, walletBetResultService);
        this.betRollbackService = betRollbackService;
        this.rawBetDetailsProducer = rawBetDetailsProducer;
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
                    (ctx, resp) -> {
                        enrichResponse(resp, request);
                        emitRawBetDetail(ctx);
                    });
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

    private void emitRawBetDetail(BetResultContext context) {
        String vendorBetId = context.getVendorBetId();
        String roundId = context.getRoundId();
        LogContext logContext = LogContextHolder.get();
        if (logContext == null || !(logContext.getBody() instanceof String requestBody)) {
            log.warn("Skipping {} raw bet detail emit: raw body unavailable vendorBetId={} roundId={}",
                    EndPoints.VENDOR, vendorBetId, roundId);
            return;
        }
        String gaBetId = RawBetDetailGaBetIdResolver.resolve(logContext.getApiBody()).orElse(null);
        if (gaBetId == null) {
            log.warn("Skipping {} raw bet detail emit: operator betId unavailable vendorBetId={} roundId={}",
                    EndPoints.VENDOR, vendorBetId, roundId);
            return;
        }
        try {
            rawBetDetailsProducer.emit(BetDetailEmitRequest.builder()
                    .vendor(EndPoints.VENDOR)
                    .eventKind(EventKind.RESULT_UPDATE)
                    .vendorBetId(vendorBetId)
                    .gaBetId(gaBetId)
                    .roundId(roundId)
                    .vendorPlayerUsername(context.getVendorPlayerUsername())
                    .agentId(context.getAgentId())
                    .gameCategoryId(context.getGameCategoryId())
                    .bodyFormat(EndPoints.BODY_FORMAT)
                    .requestBody(requestBody)
                    .build());
        } catch (Exception e) {
            log.warn("{} raw bet detail emit failed vendorBetId={} roundId={}: {}",
                    EndPoints.VENDOR, vendorBetId, roundId, e.getMessage());
        }
    }
}
