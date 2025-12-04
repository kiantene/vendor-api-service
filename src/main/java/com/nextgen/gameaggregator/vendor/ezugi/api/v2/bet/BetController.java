package com.nextgen.gameaggregator.vendor.ezugi.api.v2.bet;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.bet.AbstractBetController;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService;
import com.nextgen.gameaggregator.vendor.ezugi.api.v2.tip.TipService;
import com.nextgen.gameaggregator.vendor.ezugi.constant.BetTypeID;
import com.nextgen.gameaggregator.vendor.ezugi.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetController extends AbstractBetController<BetRequest, BetResponse> {
    private final TipService tipService;

    public BetController(BetRequestMapper requestMapper,
                         BetResponseMapper responseMapper,
                         WalletBetService walletBetService,
                         TipService tipService) {
        super(requestMapper, responseMapper, walletBetService);
        this.tipService = tipService;
    }

    @PostMapping(path = EndPoints.DEBIT)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<BetResponse> bet(@Valid @RequestBody BetRequest request) {
        BetResponse response = !isTip(request)
                ? processRequest(request, (context, betResponse) -> enrichResponse(betResponse, request))
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
}
