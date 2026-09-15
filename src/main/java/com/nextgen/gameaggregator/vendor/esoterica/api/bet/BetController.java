package com.nextgen.gameaggregator.vendor.esoterica.api.bet;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.bet.AbstractBetController;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetConfig;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.vendor.esoterica.api.betandresult.service.BetAndResultService;
import com.nextgen.gameaggregator.vendor.esoterica.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.esoterica.constant.StringConstants;
import com.nextgen.gameaggregator.vendor.esoterica.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetController extends AbstractBetController<BetRequest, CommonResponse> {

//    private final BetAndResultService betAndResultService;
//    private final GameLaunchDataService gameLaunchDataService;
//    private final GameSessionService gameSessionService;

    protected BetController(BetRequestMapper requestMapper,
                            BetResponseMapper responseMapper,
                            WalletBetService walletService) {
//                            BetAndResultService betAndResultService,
//                            GameLaunchDataService gameLaunchDataService,
//                            GameSessionService gameSessionService) {
        super(requestMapper, responseMapper, walletService);
//        this.betAndResultService = betAndResultService;
//        this.gameLaunchDataService = gameLaunchDataService;
//        this.gameSessionService = gameSessionService;
    }

    @PostMapping(EndPoints.BET)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CommonResponse> bet (@Valid @RequestBody BetRequest request) {

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
//            // call betAndResult API flow if vendor only called bet without result to avoid unsettled bet
//            return betAndResultService.bet(request);
//        }
//        else {
            // call normal bet API and result API flow
            return ResponseEntity.ok(processRequest(request));
//        }
    }

    @Override
    protected void configure(BetConfig config, BetRequest request) {
        config.allowMultipleBet(false);
    }
}
