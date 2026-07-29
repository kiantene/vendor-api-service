package com.nextgen.gameaggregator.vendor.koolbet.api.v2.sessionbet;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.bet.AbstractBetController;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetConfig;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetServiceWrapper;
import com.nextgen.gameaggregator.vendor.koolbet.api.v2.sessionresult.BetResultService;
import com.nextgen.gameaggregator.vendor.koolbet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.koolbet.constant.Formats;
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
public class SessionBetAndResultController extends AbstractBetController<SessionBetAndResultRequest, CommonResponse> {
    private final BetResultService betResultService;

    public SessionBetAndResultController(SessionBetAndResultRequestMapper requestMapper,
                                         SessionBetAndResultResponseMapper responseMapper,
                                         WalletBetServiceWrapper walletService,
                                         BetResultService betResultService) {
        super(requestMapper, responseMapper, walletService);
        this.betResultService = betResultService;
    }

    @PostMapping(path = EndPoints.SESSION_BET + "/v2")
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CommonResponse> result(@Valid @RequestBody SessionBetAndResultRequest request) {
        if (!isWagersTimeValid(request.getWagersTime())) {
            throw new InternalServerException("wagersTime expired");
        }

        int type = request.getType();

        if (type == Formats.SESSION_BET_TYPE_BET) {
            return ResponseEntity.ok(processRequest(request, (context, resp) -> enrichResponse(resp, request)));
        } else if (type == Formats.SESSION_BET_TYPE_SETTLE) {
            return betResultService.result(request);
        } else {
            throw new InternalServerException();
        }
    }

    private void enrichResponse(CommonResponse response, SessionBetAndResultRequest request) {
        response.setCurrency(request.getCurrency());
    }

    @Override
    protected void configure(BetConfig config, SessionBetAndResultRequest request) {
        config.allowMultipleBet(true);
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