package com.nextgen.gameaggregator.vendor.hp100.api.authenticate;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AbstractAuthenticateController;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateService;
import com.nextgen.gameaggregator.vendor.hp100.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.hp100.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class AuthenticateController extends AbstractAuthenticateController<AuthenticateRequest, SuccessResponse> {

    protected AuthenticateController(AuthenticateRequestMapper requestMapper,
                                     AuthenticateResponseMapper responseMapper,
                                     AuthenticateService authenticateService) {
        super(requestMapper, responseMapper, authenticateService);
    }

    @PostMapping(path = Endpoints.AUTHENTICATE)
    @VendorExceptionHandler(className = Endpoints.CLASS_NAME)
    public ResponseEntity<SuccessResponse> authenticate(@Valid @RequestBody AuthenticateRequest request) {
        SuccessResponse response = processRequest(request);
        return ResponseEntity.ok(response);
    }
}
