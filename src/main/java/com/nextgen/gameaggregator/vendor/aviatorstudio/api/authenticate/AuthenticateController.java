package com.nextgen.gameaggregator.vendor.aviatorstudio.api.authenticate;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AbstractAuthenticateController;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateService;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatorstudio.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class AuthenticateController extends AbstractAuthenticateController<AuthenticateRequest, SuccessResponse> {
    public AuthenticateController(AuthenticateRequestMapper requestMapper,
                                  AuthenticateResponseMapper responseMapper,
                                  AuthenticateService authenticateService) {
        super(requestMapper, responseMapper, authenticateService);
    }

    @GetMapping(path = EndPoints.AUTHENTICATE)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<SuccessResponse> authenticate(
            @Valid @ModelAttribute AuthenticateRequest request) {

        SuccessResponse response = processRequest(request);
        return ResponseEntity.ok(response);
    }
}
