package com.nextgen.gameaggregator.vendor.egtdigital.api.regenerate;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.game.session.AbstractGameSessionRefreshController;
import com.nextgen.gameaggregator.core.engine.game.session.GameSessionRefreshContext;
import com.nextgen.gameaggregator.core.engine.game.session.GameSessionRefreshService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.vendor.egtdigital.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.egtdigital.constant.ResponseCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class RegenerateController extends AbstractGameSessionRefreshController<RegenerateRequest, RegenerateResponse> {

    public RegenerateController(GameSessionRefreshService refreshService) {
        super(refreshService);
    }

    @PostMapping(path = EndPoints.DEFENCE)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME + "defenceCode")
    public ResponseEntity<RegenerateResponse> regenerate(@RequestBody RegenerateRequest request) {
        return ResponseEntity.ok(processRequest(request));
    }

    @Override
    protected GameSessionRefreshContext mapToInternal(RegenerateRequest request) {
        return GameSessionRefreshContext.builder()
                .vendorGameCode(request.getGameKey())
                .vendorSessionToken(request.getSessionId())
                .vendorPlayerUsername(request.getPlayerId())
                .build();
    }

    @Override
    protected RegenerateResponse mapToVendor(RegenerateRequest request,
                                             GameSessionRefreshContext context,
                                             GameSession data) {
        return RegenerateResponse.builder()
                .statusCode(ResponseCodes.OK.getCode())
                .defenceCode(data.getToken())
                .build();
    }
}
