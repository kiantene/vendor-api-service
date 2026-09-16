package com.nextgen.gameaggregator.vendor.casinogate.api.win;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.vendor.casinogate.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.casinogate.response.CommonResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class WinController extends AbstractBetResultController<WinRequest, CommonResponse> {
    public WinController(WinRequestMapper requestMapper,
                         WinResponseMapper responseMapper,
                         WalletBetResultServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = Endpoints.WIN)
    @VendorExceptionHandler(className = Endpoints.CLASS_NAME)
    public ResponseEntity<CommonResponse> win(@Valid @RequestBody WinRequest request) {
        return ResponseEntity.ok(processRequest(request));
    }

    @Override
    protected void configure(BetResultConfig config, WinRequest request) {

        config.betAndResult(false)
                .settleType(SettleType.BET)
                .allowResultWhenRoundHasEnded(false)
                .returnSuccessOnDuplicate(false)
                .rejectResultIfRefunded(true);
    }
}
