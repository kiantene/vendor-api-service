package com.nextgen.gameaggregator.vendor.spribe.api.v2.bet;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.bet.AbstractBetController;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetConfig;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService;
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
// TODO(GA-14579): the "/v2" suffix is a temporary version split for the v1 -> v2 migration.
// When v1 is retired and v2 becomes the default path, remove "/v2" here (and from the other
// spribe v2 controllers) AND update SpribeSignatureValidator.shouldValidate(), which gates
// signature validation on the literal "/v2/" — drop/adjust that gate at the same time, or
// Spribe signature validation will silently stop triggering.
@RequestMapping(path = Endpoints.PATH + "/v2")
public class BetController extends AbstractBetController<BetRequest, SuccessResponse> {
    public BetController(BetRequestMapper requestMapper,
                         BetResponseMapper responseMapper,
                         WalletBetService walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = Endpoints.WITHDRAW)
    @VendorExceptionHandler(className = SpribeConfig.CLASS_NAME)
    public ResponseEntity<SuccessResponse> bet(@Valid @RequestBody BetRequest request) {
        return ResponseEntity.ok(processRequest(
                request,
                (context, betResponse) -> enrichResponse(request, betResponse, context)
        ));
    }

    @Override
    public void configure(BetConfig config, BetRequest request) {

    }

    private void enrichResponse(BetRequest request, SuccessResponse response, BetContext context) {
        // TODO: OperatorTxId should map to a wallet transaction Id, but this is not supported yet so map to traceId first
        response.getData().setOperatorTxId(context.getTraceId());
        response.getData().setProvider(request.getProvider());
        response.getData().setProviderTxId(request.getProviderTxId());

        BigDecimal newBalance = response.getData().getNewBalance();
        response.getData().setOldBalance(newBalance.add(request.getAmount()));
    }
}
