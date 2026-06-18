package com.nextgen.gameaggregator.vendor.ezugi.api.v2.bet;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.bet.AbstractBetController;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.data.kafka.betdetails.BetDetailEmitRequest;
import com.nextgen.gameaggregator.data.kafka.betdetails.EventKind;
import com.nextgen.gameaggregator.data.kafka.betdetails.RawBetDetailsProducer;
import com.nextgen.gameaggregator.vendor.ezugi.api.v2.RawBetDetailGaBetIdResolver;
import com.nextgen.gameaggregator.vendor.ezugi.api.v2.tip.TipService;
import com.nextgen.gameaggregator.vendor.ezugi.constant.BetTypeID;
import com.nextgen.gameaggregator.vendor.ezugi.constant.EndPoints;
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
public class BetController extends AbstractBetController<BetRequest, BetResponse> {
    private final TipService tipService;
    private final RawBetDetailsProducer rawBetDetailsProducer;

    public BetController(BetRequestMapper requestMapper,
                         BetResponseMapper responseMapper,
                         WalletBetService walletBetService,
                         TipService tipService,
                         RawBetDetailsProducer rawBetDetailsProducer) {
        super(requestMapper, responseMapper, walletBetService);
        this.tipService = tipService;
        this.rawBetDetailsProducer = rawBetDetailsProducer;
    }

    @PostMapping(path = EndPoints.DEBIT)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<BetResponse> bet(@Valid @RequestBody BetRequest request) {
        BetResponse response = !isTip(request)
                ? processRequest(request, (context, betResponse) -> {
                    enrichResponse(betResponse, request);
                    emitRawBetDetail(context);
                })
                : tipService.doTip(request);

        return ResponseEntity.ok(response);
    }

    public boolean isTip(BetRequest request) {
        return request.getBetTypeID() != null && request.getBetTypeID() == BetTypeID.DEBIT_TIP;
    }

    private void enrichResponse(BetResponse response, BetRequest request) {
        response.setOperatorId(request.getOperatorId());
        response.setRoundId(request.getRoundId());
    }

    private void emitRawBetDetail(BetContext context) {
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
                    .eventKind(EventKind.PLACE_BET)
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
