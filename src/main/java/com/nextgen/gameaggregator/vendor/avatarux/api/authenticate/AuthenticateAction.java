package com.nextgen.gameaggregator.vendor.avatarux.api.authenticate;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.avatarux.constant.Credentials;
import com.nextgen.gameaggregator.vendor.avatarux.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.avatarux.constant.Headers;
import com.nextgen.gameaggregator.vendor.avatarux.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.avatarux.service.VendorService;
import com.nextgen.gameaggregator.vendor.avatarux.vo.ErrorVo;
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
    private final WalletService walletService;

    public AuthenticateAction(HttpService httpService, VendorLineService vendorLineService, VendorService vendorService, GameSessionService gameSessionService, WalletService walletService) {
        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.vendorService = vendorService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
    }

    @PostMapping(path = EndPoints.AUTHENTICATE)
    public AuthenticateVo authenticate(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String body = httpRequestLog.getRequestBody();
        String serverAuthorization = request.getHeader(Headers.SERVER_AUTHORIZATION);
        AuthenticateVo responseVo = new AuthenticateVo();
        httpRequestLog.setRequestBody("Request Body: \n" + body + "\nRequest Header: \n" + vendorService.getHeaders(request));

        try {
            // 1. Retrieve request body and convert into dto
            AuthenticateDto dto = HttpService.convertJsonToDto(body, AuthenticateDto.class);
            dto.setXServerAuthorization(serverAuthorization);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 3. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getOperator());

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession, body);

            BigDecimal balance = getCurrentBalance(httpRequestLog.getId(), gameSession, httpRequestLog);

            // 5. Set response data
            responseVo.setNativeId(dto.getOperator());
            responseVo.setToken(gameSession.getToken());
            responseVo.setBalance(balance);
            responseVo.setCurrency(gameSession.getCurrencyCode().toLowerCase());
            responseVo.setBrand("ONEAPI");

        } catch (AuthenticationException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setError(new ErrorVo());
            responseVo.getError().setCode(ResponseCode.SERVER_UNAUTHORIZED.code);
            responseVo.getError().setMessage(ResponseCode.SERVER_UNAUTHORIZED.description);
        } catch (InvalidPlayerException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setError(new ErrorVo());
            responseVo.getError().setCode(ResponseCode.PLAYER_UNAUTHORIZED.code);
            responseVo.getError().setMessage(ResponseCode.PLAYER_UNAUTHORIZED.description);
        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setError(new ErrorVo());
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

    private void doVerification(AuthenticateDto dto, GameSession gameSession, String body) throws AuthenticationException, CredentialNotFoundException, InvalidPlayerException {        // Verify received signature
        //1. Verify X-Server-Authorization
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
        ValidationUtils.isEquals(VendorService.generateHash(secretKey, body), dto.getXServerAuthorization(), AuthenticationException::new);

        //2. Verify Authorization
        ValidationUtils.isEquals(gameSession.getToken(), dto.getKey(), InvalidPlayerException::new);
    }

    private BigDecimal getCurrentBalance(String traceId, GameSession gameSession, final HttpRequestLog httpRequestLog) throws InvalidAgentApiCredentialException, VendorCurrencyNotSupportException, InvalidOperatorResponseException {
        HttpRequestLog httpRequestLogdup = new HttpRequestLog(httpRequestLog);

        // Call the service with the duplicate log
        return walletService.getBalance(traceId, gameSession, httpRequestLogdup);
    }

}
