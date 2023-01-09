package com.nextgen.gameaggregator.vendor.pragmaticplay.api.authenticate;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.*;
import com.nextgen.gameaggregator.vendor.pragmaticplay.service.VendorService;
import com.nextgen.gameaggregator.vendor.pragmaticplay.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

@RestController
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
public class AuthenticateAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;

    @PostMapping(path = Endpoints.AUTHENTICATE)
    public ResponseVo authenticate(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        AuthenticateVo responseVo = new AuthenticateVo();
        String traceId = httpRequestLog.getTraceId();

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            AuthenticateDto dto = HttpService.convertQueryStringToDto(body, AuthenticateDto.class);

            // 1. Validate request parameters from vendor
            ValidationUtils.validateRequest(dto);
            ValidationUtils.isEquals(dto.getProviderId(), Credentials.PROVIDER_ID);

            // 2. Verify session token
            // Need to retrieve line credentials from game session in order to validate hash
            // If Token has been tampered, then AuthenticationException will be thrown
            GameSession gameSession = gameSessionService.verifyToken(dto.getToken());

            // 3. Retrieve vendor line credentials and secretKey for hash validation
            String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);

            // 4. Validate request signature
            VendorService.verifyHash(body, secretKey);

            // 5. Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            // Emit event for additional asynchronous processing
//            eventDispatcher.emit(getClass(), body);

            responseVo.setUserId(gameSession.getVendorPlayerUsername());
            responseVo.setCurrency(gameSession.getCurrencyCode());
            responseVo.setCash(balance);
            responseVo.setBonus(BigDecimal.ZERO);
            responseVo.setToken(gameSession.getToken());

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setError(ResponseCodes.INVALID_REQUEST);
            if (invalidRequestException.getValidation() != null) {
                httpRequestLog.setErrorMessage(invalidRequestException.getValidation().toString());
            }

        } catch (AuthenticationException authenticationException) {
            responseVo.setError(ResponseCodes.AUTHENTICATION_ERROR);

        } catch (InvalidSignatureException invalidSignatureException) {
            responseVo.setError(ResponseCodes.INVALID_HASH);

        } catch (CredentialNotFoundException credentialNotFoundException) {
            responseVo.setError(ResponseCodes.INTERNAL_SERVER_ERROR_NO_RETRY);
            httpService.logError(httpRequestLog, credentialNotFoundException);

        } catch (Exception exception) { // any other exception encountered
            responseVo.setError(ResponseCodes.INTERNAL_SERVER_ERROR_NO_RETRY);
            httpService.logError(httpRequestLog, exception);

        } finally {
            responseVo.setDescription( ResponseCodes.RESPONSE_DESCRIPTION.get(responseVo.getError()));
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }
}
