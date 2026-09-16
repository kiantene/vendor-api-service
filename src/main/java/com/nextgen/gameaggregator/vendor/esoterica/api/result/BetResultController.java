package com.nextgen.gameaggregator.vendor.esoterica.api.result;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.vendor.esoterica.api.result.betandresult.BetAndResultService;
import com.nextgen.gameaggregator.vendor.esoterica.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.esoterica.constant.StringConstants;
import com.nextgen.gameaggregator.vendor.esoterica.response.CommonResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetResultController extends AbstractBetResultController<BetResultRequest, CommonResponse> {

//    private final BetAndResultService betAndResultService;
//    private final GameLaunchDataService gameLaunchDataService;
//    private final GameSessionService gameSessionService;

    protected BetResultController(BetResultRequestMapper requestMapper,
                                  BetResultResponseMapper responseMapper,
                                  WalletBetResultServiceWrapper walletService) {
//                                  BetAndResultService betAndResultService,
//                                  GameLaunchDataService gameLaunchDataService,
//                                  GameSessionService gameSessionService) {
        super(requestMapper, responseMapper, walletService);
//        this.betAndResultService = betAndResultService;
//        this.gameLaunchDataService = gameLaunchDataService;
//        this.gameSessionService = gameSessionService;
    }

    @PostMapping(path = EndPoints.RESULT)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CommonResponse> result(@Valid @RequestBody BetResultRequest request) {

//        String subcategoryCode;
//
//        try {
//
//            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(request.getUserId(), request.getGameName());
//            subcategoryCode = gameLaunchDataService.getBackfacingGameSubcategoryByVendorGameCodeAndVendorId(request.getGameName(), gameSession.getVendorId())
//                    .map(GameLaunchDataService.GameSubcategoryInfo::code)
//                    .orElse("");
//        } catch (AuthenticationException e) {
//            throw new RuntimeException(e);
//        }
//
//        if (subcategoryCode.equals(StringConstants.ES_TYPE2)) {
//            return betAndResultService.result(request);
//        }
//        else {
            return ResponseEntity.ok(processRequest(request));
//        }
    }

    @Override
    protected void configure(BetResultConfig config, BetResultRequest request) {
        config.settleType(SettleType.ROUND).setAllowResultWhenRoundHasEnded(false);
    }
}
