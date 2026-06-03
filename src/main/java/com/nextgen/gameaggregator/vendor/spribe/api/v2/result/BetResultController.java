package com.nextgen.gameaggregator.vendor.spribe.api.v2.result;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.vendor.spribe.config.SpribeConfig;
import com.nextgen.gameaggregator.vendor.spribe.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.spribe.constant.FreeBetAction;
import com.nextgen.gameaggregator.vendor.spribe.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping(path = Endpoints.PATH + "/v2")
public class BetResultController extends AbstractBetResultController<BetResultRequest, SuccessResponse> {
    private final List<DepositActionHandler> depositActionHandlers;

    public BetResultController(BetResultRequestMapper requestMapper,
                               BetResultResponseMapper responseMapper,
                               WalletBetResultServiceWrapper walletBetResultService,
                               List<DepositActionHandler> depositActionHandlers) {
        super(requestMapper, responseMapper, walletBetResultService);
        this.depositActionHandlers = depositActionHandlers;
    }

    @PostMapping(path = Endpoints.DEPOSIT)
    @VendorExceptionHandler(className = SpribeConfig.CLASS_NAME)
    public ResponseEntity<SuccessResponse> result(@Valid @RequestBody BetResultRequest request) {
        return ResponseEntity.ok(
            depositActionHandlers.stream()
                .filter(h -> h.supports(request.getAction()))
                .findFirst()
                .map(h -> h.handle(request))
                .orElseGet(() -> processRequest(request,
                    (ctx, resp) -> enrichResponse(request, resp, ctx)))
        );
    }

    @Override
    public void configure(BetResultConfig config, BetResultRequest request) {
        config.settleType(SettleType.BET);
        if (FreeBetAction.list.contains(request.getAction())) {
            config.setBetAndResult(true);
        }
    }

    private void enrichResponse(BetResultRequest request, SuccessResponse response, BetResultContext context) {
        // TODO: OperatorTxId should map to a wallet transaction Id, but this is not supported yet so map to traceId first
        response.getData().setOperatorTxId(context.getTraceId());
        response.getData().setProvider(request.getProvider());
        response.getData().setProviderTxId(request.getProviderTxId());

        BigDecimal newBalance = response.getData().getNewBalance();
        response.getData().setOldBalance(newBalance.subtract(request.getAmount()));
    }
}
