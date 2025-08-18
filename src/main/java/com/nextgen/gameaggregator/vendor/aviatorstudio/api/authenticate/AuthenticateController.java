package com.nextgen.gameaggregator.vendor.aviatorstudio.api.authenticate;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AbstractAuthenticateController;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateService;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
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

    @GetMapping(path = EndPoints.AUTHENTICATE)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<AuthenticateResponse> authenticate(
            @Valid @ModelAttribute AuthenticateRequest request,
            @RequestAttribute("token") String token,
            @RequestAttribute("username") String username) {

        AuthenticateResponse response = processRequest(
                request,
                context -> enrichContext(context, token, username)
        );
        return ResponseEntity.ok(response);
    }

    private void enrichContext(AuthenticateContext context, String token, String username) {
        context.setToken(token);
        context.setVendorPlayerUsername(username);
    }
}
