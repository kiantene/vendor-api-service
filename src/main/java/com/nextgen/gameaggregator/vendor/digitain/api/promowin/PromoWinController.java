package com.nextgen.gameaggregator.vendor.digitain.api.promowin;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.vendor.digitain.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class PromoWinController extends AbstractBetResultController<PromoWinRequest, PromoWinResponse> {

    public PromoWinController(PromoWinRequestMapper requestMapper,
                              PromoWinResponseMapper responseMapper,
                              WalletBetResultServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = EndPoints.PROMOWIN)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME+"PromoWin")
    public ResponseEntity<PromoWinResponse> result(@Valid @RequestBody PromoWinRequest request,
                                                   @RequestHeader(value = "SecretKey", required = true) String authorization) {

        PromoWinResponse response = processRequest(request);
        return ResponseEntity.ok()
                .header("SecretKey", authorization)
                .body(response);
    }

    @Override
    public void configure(BetResultConfig config, PromoWinRequest request) {
        config.betAndResult(true)
                .allowResultBeforeBet(true)
                .setSettleType(SettleType.BET);
    }
}