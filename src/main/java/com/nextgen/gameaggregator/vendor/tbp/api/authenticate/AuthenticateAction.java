package com.nextgen.gameaggregator.vendor.tbp.api.authenticate;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.tbp.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.tbp.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.tbp.service.VendorService;
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
    private final VendorService vendorService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;

    public AuthenticateAction(HttpService httpService, VendorService vendorService, GameSessionService gameSessionService, WalletService walletService) {
        this.httpService = httpService;
        this.vendorService = vendorService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
    }

    @PostMapping(path = EndPoints.AUTHENTICATE)
    public AuthenticateVo authenticate(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String body = httpRequestLog.getRequestBody();
        AuthenticateVo responseVo = new AuthenticateVo();

        try {
            // 1. Retrieve request body and convert into dto
            AuthenticateDto dto = HttpService.convertJsonToDto(body, AuthenticateDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 3. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getPlayerId());
            gameSession.setVendorToken(dto.getSessionId());
            gameSessionService.updateSession(gameSession);

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);

            BigDecimal balance = getCurrentBalance(httpRequestLog.getId(), gameSession, httpRequestLog);

            // 5. Set response data
            responseVo.setUsername(dto.getUsername());
            responseVo.setPassword(dto.getPassword());
            responseVo.setSessionId(dto.getSessionId());
            responseVo.setUserId(dto.getPlayerId());
            responseVo.setCurrency(gameSession.getCurrencyCode());
            responseVo.setBalance(balance);
            responseVo.setError(ResponseCode.OK);

        } catch (InvalidRequestException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setError(ResponseCode.UNEXPECTED_INPUT);

        } catch (AuthenticationException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setError(ResponseCode.PERMISSION_DENIED);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setError(ResponseCode.INTERNAL_SERVER_ERROR);

        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(AuthenticateDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(AuthenticateDto authenticateDto, GameSession gameSession) throws AuthenticationException, CredentialNotFoundException {
        //1. verify Username, Password, PlayerId
        vendorService.validate(authenticateDto.getUsername(), authenticateDto.getPassword(), authenticateDto.getPlayerId(), gameSession);

        //2. Verify DefenceCode
        ValidationUtils.isEquals(gameSession.getToken(), authenticateDto.getDefenceCode(), AuthenticationException::new);
    }

    private BigDecimal getCurrentBalance(String traceId, GameSession gameSession, final HttpRequestLog httpRequestLog) throws InvalidAgentApiCredentialException, VendorCurrencyNotSupportException, InvalidOperatorResponseException {
        HttpRequestLog httpRequestLogDup = new HttpRequestLog(httpRequestLog);

        // Call the service with the duplicate log
        return walletService.getBalance(traceId, gameSession, httpRequestLogDup);
    }

}
