package com.nextgen.gameaggregator.vendor.spribe.api.v2.authenticate;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AbstractAuthenticateController;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthConfig;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateService;
import com.nextgen.gameaggregator.vendor.spribe.config.SpribeConfig;
import com.nextgen.gameaggregator.vendor.spribe.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.spribe.response.BalanceResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class AuthController extends AbstractAuthenticateController<AuthRequest, BalanceResponse> {
    public AuthController(AuthRequestMapper requestMapper,
                          AuthResponseMapper responseMapper,
                          AuthenticateService authService) {
        super(requestMapper, responseMapper, authService);
    }

    @PostMapping(path = Endpoints.AUTHENTICATE)
    @VendorExceptionHandler(className = SpribeConfig.CLASS_NAME)
    public ResponseEntity<BalanceResponse> authenticate(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(processRequest(request));
    }

    @Override
    public void configure(AuthConfig config, AuthRequest request) {
        config.refreshToken(true)
                .replaceTokenWith(request.getSessionToken());
    }
}
