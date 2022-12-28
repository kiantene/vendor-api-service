package com.nextgen.gameaggregator.vendor.pgsoft.api.authenticate;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.event.EventDispatcher;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.NoAvailableLineException;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pgsoft.service.VendorService;
import com.nextgen.gameaggregator.vendor.pgsoft.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

@RestController
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
public class VerifySessionAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private EventDispatcher eventDispatcher;


    @PostMapping(path = Endpoints.AUTHENTICATE)
    public ResponseVo<VerifySessionVo> authenticate(HttpServletRequest request) {
        ResponseVo<VerifySessionVo> parentResponseVo = new ResponseVo<>();
        VerifySessionVo responseVo = new VerifySessionVo();
        parentResponseVo.setData(responseVo);

        HttpRequestLog httpRequestLog = httpService.logRequest(request);
        String traceId = UUID.randomUUID().toString();

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();
            // Convert original request body into dto
            VerifySessionDto dto = HttpService.convertQueryStringToDto(body, VerifySessionDto.class);

            // 1. Validate request parameters from vendor
            ValidationUtils.validateRequest(dto);

            // 2. Verify session token
            // Need to retrieve line credentials from game session in order to validate hash
            // If Token has been tampered, then AuthenticationException will be thrown
            GameSession gameSession = gameSessionService.verifyToken(dto.getOperatorPlayerSession());

            // 3. Retrieve vendor line operatorToken and secretKey for validation
            String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
            String operatorToken = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.OPERATOR_TOKEN);

            // 4. Validate request operatorToken and secretKey
            VendorService.validateOperatorTokenAndSecretKey(dto.getOperatorToken(), dto.getSecretKey(), operatorToken, secretKey);

            // Emit event for additional asynchronous processing
            eventDispatcher.emit(getClass(), body);

            // Fill VO required values
            responseVo.setPlayerName(gameSession.getVendorPlayerUsername());
            responseVo.setCurrency(gameSession.getCurrencyCode());

        } catch (AuthenticationException authenticationException) {
            parentResponseVo.setError(ResponseCodes.INVALID_REQUEST);

        } catch (NoAvailableLineException noAvailableLineException) {
            parentResponseVo.setError(ResponseCodes.INVALID_REQUEST);

        } catch (Exception exception) { // any other exception encountered
            parentResponseVo.setError(ResponseCodes.INTERNAL_SERVER_ERROR);
            httpRequestLog.setErrorMessage(HttpService.getStackTrace(exception));

        } finally {
            if (parentResponseVo.getError() != null) {
                httpRequestLog.setStatus(HttpService.ERROR);
            }
            httpRequestLog.setEndTime(System.currentTimeMillis());
            ConcurrencyService.THREAD_POOL.submit(() -> httpService.logResponse(httpRequestLog, responseVo, traceId));
        }

        //
        return parentResponseVo;
    }
}
