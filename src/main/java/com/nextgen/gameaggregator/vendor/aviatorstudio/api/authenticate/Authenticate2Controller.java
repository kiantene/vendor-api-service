package com.nextgen.gameaggregator.vendor.aviatorstudio.api.authenticate;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateServiceWrapper;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatorstudio.service.VendorService;
import com.nextgen.gameaggregator.vendor.aviatorstudio.vo.CommonVo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.nextgen.gameaggregator.vendor.aviatorstudio.validator.AviatorStudioSignatureValidator.HEADER_AUTHORIZATION;

@RestController
@RequestMapping(path = EndPoints.PATH)
@RequiredArgsConstructor
public class Authenticate2Controller {
    private final AuthenticateRequestMapper requestMapper;
    private final AuthenticateResponseMapper responseMapper;
    private final AuthenticateServiceWrapper authenticateService;

    @GetMapping(path = EndPoints.AUTHENTICATE + "/v2")
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CommonVo> account(
            @RequestHeader(HEADER_AUTHORIZATION) String jwt,
            @ModelAttribute AuthenticateDto dto) {

        AuthenticateContext context = requestMapper.toAuthenticateContext(dto);
        enrich(context, jwt);
        PlayerBalanceData balanceData = authenticateService.process(context);
        return ResponseEntity.ok(responseMapper.toVendor(context, balanceData));
    }

    private void enrich(AuthenticateContext context, String jwt) {
        context.setVendorPlayerUsername(VendorService.jwtGetUserId(jwt));
    }
}
