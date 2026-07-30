package com.nextgen.gameaggregator.vendor.koolbet.api.v2.authenticate;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AbstractAuthenticateController;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateService;
import com.nextgen.gameaggregator.vendor.koolbet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.koolbet.response.CommonResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class AuthenticateController extends AbstractAuthenticateController<AuthenticateRequest, CommonResponse> {
    protected AuthenticateController(AuthenticateRequestMapper requestMapper,
                                     AuthenticateResponseMapper responseMapper,
                                     AuthenticateService authenticateService) {
        super(requestMapper, responseMapper, authenticateService);
    }

    @PostMapping(path = EndPoints.TOKEN + "/v2")
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CommonResponse> authenticate(
            @Valid @RequestBody AuthenticateRequest request) {
        return ResponseEntity.ok(processRequest(request));
    }
}
