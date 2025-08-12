package com.nextgen.gameaggregator.vendor.aviatorstudio.api.authenticate;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateServiceWrapper;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatorstudio.vo.CommonVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = EndPoints.PATH)
@RequiredArgsConstructor
public class AuthenticateController {
    private final AuthenticateRequestMapper requestMapper;
    private final AuthenticateResponseMapper responseMapper;
    private final AuthenticateServiceWrapper authenticateService;

    @GetMapping(path = EndPoints.AUTHENTICATE)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<AuthenticateResponse> authenticate(
            @Valid @ModelAttribute AuthenticateRequest request,
            @RequestAttribute("token") String token,
            @RequestAttribute("username") String username) {

        AuthenticateContext context = requestMapper.toAuthenticateContext(request);
        enrich(context, token, username);
        PlayerBalanceData balanceData = authenticateService.process(context);
        return ResponseEntity.ok(responseMapper.toVendor(context, balanceData));
    }

    private void enrich(AuthenticateContext context, String token, String username) {
        context.setToken(token);
        context.setVendorPlayerUsername(username);
    }
}
