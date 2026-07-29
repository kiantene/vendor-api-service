package com.nextgen.gameaggregator.vendor.egtdigital.api.authenticate;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AbstractAuthenticateController;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthConfig;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateService;
import com.nextgen.gameaggregator.vendor.egtdigital.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class AuthenticateController extends AbstractAuthenticateController<AuthenticateRequest, AuthenticateResponse> {
    public AuthenticateController(AuthenticateRequestMapper requestMapper,
            AuthenticateResponseMapper responseMapper,
            AuthenticateService authenticateService) {
        super(requestMapper, responseMapper, authenticateService);
    }

    @PostMapping(path = EndPoints.AUTHENTICATE)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<AuthenticateResponse> authenticate(
            @Valid @RequestBody AuthenticateRequest request,
        @RequestHeader(value = "X-Checksum", required = true) String checkSum,
        @RequestHeader(value = "X-Checksum-Fields", required = true) String checkSumFields){

        AuthenticateResponse response = processRequest(request);
        return ResponseEntity.ok()
                .header("X-Checksum", checkSum)
                .header("X-Checksum-Fields",checkSumFields)
                .body(response);
    }

    @Override
    public void configure(AuthConfig config, AuthenticateRequest request) {
        config.refreshToken(true)
                .replaceTokenWith(request.getSessionId());
    }

}
