package com.nextgen.gameaggregator.vendor.pragmaticplay.api.endround;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.event.EventDispatcher;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.pragmaticplay.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
public class EndRoundAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private EventDispatcher eventDispatcher;

    @PostMapping(path = Endpoints.END_ROUND)
    public EndRoundVo endRound(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.logRequest(request);
        EndRoundVo responseVo = new EndRoundVo();
        String traceId = UUID.randomUUID().toString();

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            EndRoundDto dto = HttpService.convertQueryStringToDto(body, EndRoundDto.class);

            // 1. Validate request parameters from vendor
            ValidationUtils.validateRequest(dto);
            ValidationUtils.validateVendorUsername(dto.getUserId());
            ValidationUtils.validateEquals(dto.getProviderId(), Credentials.PROVIDER_ID);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getToken());

            // 3. Retrieve vendor line credentials and secretKey for hash validation
            String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);

            // 4. Validate request signature
            VendorService.validateHash(body, secretKey);

            // 5. Retrieve the latest wallet balance from Operator
            // TODO: performance tuning, may cache the last balance from Result and use that
            //  last balance to return to vendor, instead of making another call to Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            // Emit event for additional asynchronous processing
            eventDispatcher.emit(getClass(), body);

            responseVo.setCash(balance);
            responseVo.setBonus(BigDecimal.ZERO);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setError(ResponseCodes.INVALID_REQUEST);
            httpRequestLog.setErrorMessage(invalidRequestException.getValidation().toString());

        } catch (AuthenticationException authenticationException) {
            responseVo.setError(ResponseCodes.AUTHENTICATION_ERROR);

//        } catch (UnableToFindCredentialsException unableToFindCredentialsException) {
//            responseVo.setError(ResponseCodes.INTERNAL_SERVER_ERROR_NO_RETRY);

        } catch (InvalidSignatureException invalidSignatureException) {
            responseVo.setError(ResponseCodes.INVALID_HASH);

        } catch (Exception exception) { // any other exception encountered
            responseVo.setError(ResponseCodes.INTERNAL_SERVER_ERROR_NO_RETRY);
            httpRequestLog.setErrorMessage(HttpService.getStackTrace(exception));

        } finally {
            responseVo.setDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(responseVo.getError()));
            if (!responseVo.getError().equals(ResponseCodes.SUCCESS)) {
                httpRequestLog.setStatus(HttpService.ERROR);
            }
            httpRequestLog.setEndTime(System.currentTimeMillis());
            ConcurrencyService.THREAD_POOL.submit(() -> httpService.logResponse(httpRequestLog, responseVo, traceId));
        }

        return responseVo;
    }
}
