package com.nextgen.gameaggregator.vendor.aviatorstudio.api.authenticate;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatorstudio.service.VendorService;
import com.nextgen.gameaggregator.vendor.aviatorstudio.validator.AviatorStudioSignatureValidator;
import com.nextgen.gameaggregator.vendor.aviatorstudio.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@RequiredArgsConstructor
public class AuthenticateController {

    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;

//    @GetMapping(path = EndPoints.AUTHENTICATE)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CommonVo> account(HttpServletRequest request) throws Exception {
        HttpRequestLog httpRequestLog = httpService.start(request);
        CommonVo responseVo = new CommonVo();
        HttpStatus status = HttpStatus.OK;

        String traceId = httpRequestLog.getId();
        String queryString = request.getQueryString();

        AuthenticateDto dto = HttpService.convertQueryStringToDto(queryString, AuthenticateDto.class);
        ValidationUtils.validateRequest(dto);
        String jwtAuth = request.getHeader(AviatorStudioSignatureValidator.HEADER_AUTHORIZATION);
        String vendorPlayerUsername = VendorService.jwtGetUserId(jwtAuth);
        GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayerUsername);
        BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);
        responseVo.setResponseSuccess(balance, gameSession.getVendorPlayerId().toString(), vendorPlayerUsername);
        httpService.end(httpRequestLog, responseVo);

        return new ResponseEntity<>(responseVo, new HttpHeaders(), status);
    }
}
