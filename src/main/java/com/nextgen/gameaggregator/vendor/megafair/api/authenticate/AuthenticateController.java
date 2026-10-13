package com.nextgen.gameaggregator.vendor.megafair.api.authenticate;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AbstractAuthenticateController;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthConfig;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateService;
import com.nextgen.gameaggregator.vendor.megafair.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class AuthenticateController extends AbstractAuthenticateController<AuthenticateRequest, AuthenticateResponse> {
    public AuthenticateController(AuthenticateRequestMapper requestMapper,
                                  AuthenticateResponseMapper responseMapper,
                                  AuthenticateService authenticateService) {
        super(requestMapper, responseMapper, authenticateService);
    }

    @PostMapping(path = EndPoints.AUTHENTICATE, produces = MediaType.APPLICATION_XML_VALUE)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<AuthenticateResponse> authenticate(@Valid @ModelAttribute AuthenticateRequest request) {
        return ResponseEntity.ok(processRequest(request, (context, resp) -> enrichResponse(resp, request)));
    }

    private void enrichResponse(AuthenticateResponse response, AuthenticateRequest request) {
        if (response.getRequest() == null) {
            response.setRequest(AuthenticateResponse.RequestData.builder().build());
        }
        response.getRequest().setToken(request.getToken());
        response.getRequest().setGameSessionId(request.getGameSessionId());
        response.getRequest().setHash(request.getHash());
    }

    @Override
    protected void configure(AuthConfig config, AuthenticateRequest request) {
        config.refreshToken(true).replaceTokenWith(request.getGameSessionId());
    }
}
