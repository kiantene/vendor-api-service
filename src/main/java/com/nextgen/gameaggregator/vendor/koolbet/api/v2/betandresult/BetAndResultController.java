package com.nextgen.gameaggregator.vendor.koolbet.api.v2.betandresult;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.vendor.koolbet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.koolbet.response.CommonResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetAndResultController extends AbstractBetResultController<BetAndResultRequest, CommonResponse> {

    public BetAndResultController(BetAndResultRequestMapper requestMapper,
                                  BetAndResultResponseMapper responseMapper,
                                  WalletBetResultServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = EndPoints.BET + "/v2")
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CommonResponse> result(@Valid @RequestBody BetAndResultRequest request) {
        if (!isWagersTimeValid(request.getWagersTime())) {
            throw new InternalServerException("wagersTime expired");
        }
        return ResponseEntity.ok(processRequest(request, (context, resp) -> enrichResponse(resp, request)));
    }

    @Override
    public void configure(BetResultConfig config, BetAndResultRequest request) {
        config.betAndResult(true);
    }

    private void enrichResponse(CommonResponse response, BetAndResultRequest request) {
        response.setCurrency(request.getCurrency());
    }

    private boolean isWagersTimeValid(Long wagersTime) {
        if (wagersTime == null) {
            return false;
        }

        long currentServerTime = Instant.now().getEpochSecond();
        long durationSeconds = currentServerTime - wagersTime;

        // Requirement: current_server_time - wagersTime > 1800s (30 minutes)
        return durationSeconds <= 1800;
    }
}