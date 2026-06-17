package com.nextgen.gameaggregator.vendor.egtdigital.api.terminate;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.game.session.terminate.AbstractTerminateGameSessionController;
import com.nextgen.gameaggregator.core.engine.game.session.terminate.TerminateGameSessionContext;
import com.nextgen.gameaggregator.core.engine.game.session.terminate.TerminateGameSessionService;
import com.nextgen.gameaggregator.vendor.egtdigital.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.egtdigital.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.egtdigital.dto.RequestCommonDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class TerminateController extends AbstractTerminateGameSessionController<RequestCommonDto, TerminateResponse> {

    public TerminateController(TerminateGameSessionService terminateGameSessionService) {
        super(terminateGameSessionService);
    }

    @PostMapping(path = EndPoints.TERMINATE)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<TerminateResponse> regenerate(@RequestBody RequestCommonDto request) {
        return ResponseEntity.ok(processRequest(request));
    }

    @Override
    protected TerminateGameSessionContext mapToInternal(RequestCommonDto request) {
        return TerminateGameSessionContext.builder()
                .vendorSessionToken(request.getSessionId())
                .vendorPlayerUsername(request.getPlayerId())
                .build();
    }

    @Override
    protected TerminateResponse mapToVendor(RequestCommonDto request, TerminateGameSessionContext context, Void data) {
        return TerminateResponse.builder()
                .statusCode(ResponseCodes.OK.getCode())
                .build();
    }
}
