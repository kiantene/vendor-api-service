package com.nextgen.gameaggregator.vendor.digitain.api.authenticate;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AbstractAuthenticateController;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateService;
import com.nextgen.gameaggregator.vendor.digitain.constant.EndPoints;
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
            @Valid @RequestBody AuthenticateRequest request, @RequestHeader(value = "SecretKey",
            required = true) String authorization) {

        AuthenticateResponse response = processRequest(request);
        return ResponseEntity.ok()
                .header("SecretKey", authorization)
                .body(response);
    }
}
