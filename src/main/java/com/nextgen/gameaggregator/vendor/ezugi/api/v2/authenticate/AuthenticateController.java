package com.nextgen.gameaggregator.vendor.ezugi.api.v2.authenticate;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.game.authenticate.*;
import com.nextgen.gameaggregator.vendor.ezugi.constant.EndPoints;

import jakarta.validation.Valid;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class AuthenticateController extends AbstractAuthenticateController<AuthenticateRequest, AuthenticateResponse> {
    public AuthenticateController(AuthenticateRequestMapper requestMapper,
                                  AuthenticateResponseMapper responseMapper,
                                  AuthenticateService authenticateService) {
        super(requestMapper, responseMapper, authenticateService);
    }

    @PostMapping(path = EndPoints.AUTHENTICATION)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<AuthenticateResponse> authenticate(@Valid @RequestBody AuthenticateRequest request) {
        AuthenticateResponse response = processRequest(
                request,
                (context, resp) -> enrichResponse(resp, request)
        );

        return ResponseEntity.ok(response);
    }

    private void enrichResponse(AuthenticateResponse response, AuthenticateRequest request) {
        response.setOperatorId(request.getOperatorId());
    }

    @Override
    public void configure(AuthConfig config, AuthenticateRequest request) {
        config.refreshToken(true)
              .replaceTokenWith(UUID.randomUUID().toString());
    }
}
