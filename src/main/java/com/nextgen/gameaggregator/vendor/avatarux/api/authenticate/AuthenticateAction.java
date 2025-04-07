package com.nextgen.gameaggregator.vendor.avatarux.api.authenticate;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.avatarux.constant.Credentials;
import com.nextgen.gameaggregator.vendor.avatarux.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.avatarux.constant.Headers;
import com.nextgen.gameaggregator.vendor.avatarux.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.avatarux.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class AuthenticateAction {
    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final VendorService vendorService;
    private final GameSessionService gameSessionService;

    public AuthenticateAction(HttpService httpService, VendorLineService vendorLineService, VendorService vendorService, GameSessionService gameSessionService) {
        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.vendorService = vendorService;
        this.gameSessionService = gameSessionService;
    }

    @PostMapping(path = EndPoints.Authenticate)
    public AuthenticateVo authenticate(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String body = httpRequestLog.getRequestBody();
        String authorization = request.getHeader(Headers.SERVER_AUTHORIZATION);
        AuthenticateVo responseVo = new AuthenticateVo();
        httpRequestLog.setRequestBody("Request Body: \n" + body + "\nRequest Header: \n" + vendorService.getHeaders(request));

        try {
            // 1. Retrieve request body and convert into dto
            AuthenticateDto dto = HttpService.convertJsonToDto(body, AuthenticateDto.class);
            dto.setXServerAuthorization(authorization);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 3. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getOperator());

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession, body, httpRequestLog.getMethod());

            // 5. Set response data
            responseVo.setNativeId(dto.getOperator());
            responseVo.setToken(gameSession.getToken());
            responseVo.setBalance(BigDecimal.ZERO);
            responseVo.setCurrency(gameSession.getCurrencyCode().toLowerCase());
            responseVo.setBrand("OneAPI");

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            responseVo.getError().setCode(ResponseCode.UNKNOWN.code);
            responseVo.getError().setMessage(ResponseCode.UNKNOWN.description);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(AuthenticateDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(AuthenticateDto dto, GameSession gameSession, String body, String method) throws AuthenticationException, CredentialNotFoundException {        // Verify received signature
        //1. Verify X-Server-Authorization
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
        ValidationUtils.isEquals(VendorService.generateHash(secretKey, body), dto.getXServerAuthorization(), AuthenticationException::new);
    }

}
