package com.nextgen.gameaggregator.vendor.pragmaticplay.api.result;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.core.EventDispatcherSystem;
import com.nextgen.gameaggregator.eventing.events.BetResultEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.ResponseCodes;
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
public class ResultAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;

    @PostMapping(path = Endpoints.RESULT)
    public ResponseVo betResult(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        ResultVo responseVo = new ResultVo();
        String traceId = httpRequestLog.getTraceId();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            ResultDto dto = HttpService.convertQueryStringToDto(body, ResultDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getToken());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, dto, gameSession);

            // 4. Send win result to Operator
            BetResultEvent betResultEvent = walletService.processWin(traceId, gameSession, dto, body);

            //TODO (by Alex), should the not found roundId pre-handle in case the insert query for bet request is under queue

            // Emit event for additional asynchronous processing
            EventDispatcherSystem.emitAsync(betResultEvent);

            responseVo.setTransactionId(traceId);
            responseVo.setCurrency(gameSession.getCurrencyCode()); // TODO: vendor currency map
            responseVo.setCash(betResultEvent.getLastBalance());
            responseVo.setBonus(BigDecimal.ZERO);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setError(ResponseCodes.INVALID_REQUEST);
            if (invalidRequestException.getValidation() != null) {
                httpRequestLog.setErrorMessage(invalidRequestException.getValidation().toString());
            }

        } catch (InvalidPlayerException invalidPlayerException) {
            responseVo.setError(ResponseCodes.PLAYER_NOT_FOUND);

        } catch (AuthenticationException authenticationException) {
            responseVo.setError(ResponseCodes.AUTHENTICATION_ERROR);

//        } catch (UnableToFindCredentialsException unableToFindCredentialsException) {
//            responseVo.setError(ResponseCodes.INTERNAL_SERVER_ERROR_NO_RETRY);

        } catch (InvalidSignatureException invalidSignatureException) {
            responseVo.setError(ResponseCodes.INVALID_HASH);

        } catch (DuplicateExternalTransactionIdException duplicateExternalTransactionIdException) {
            responseVo.setError(ResponseCodes.BET_NOT_ALLOWED);
            httpRequestLog.setErrorMessage(duplicateExternalTransactionIdException.getMessage());

        } catch (BetNotFoundException betNotFoundException) {
            responseVo.setError(ResponseCodes.BET_NOT_ALLOWED);
            httpRequestLog.setErrorMessage(betNotFoundException.getMessage());

        } catch (Exception exception) { // any other exception encountered
            responseVo.setError(ResponseCodes.INTERNAL_SERVER_ERROR_NO_RETRY);
            httpService.logError(httpRequestLog, exception);

        } finally {
            responseVo.setDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(responseVo.getError()));
            httpService.end(httpRequestLog, responseVo);
        }

        //TODO should the trace Id return for all the responses even the request is fail?
        return responseVo;
    }

    private void doValidation(ResultDto dto) throws InvalidRequestException, InvalidPlayerException {
        // General validation
        ValidationUtils.validateRequest(dto);
        // Validation with custom exception
        ValidationUtils.validateLength(dto.getUserId(), 3, 20, InvalidPlayerException::new);
        //TODO (by Alex), get the provider ID from vendor_line_credentials tables
        ValidationUtils.isEquals(dto.getProviderId(), Credentials.PROVIDER_ID);
    }

    private void doVerification(HttpRequestLog request, ResultDto dto, GameSession gameSession) throws
            InvalidPlayerException, CredentialNotFoundException, InvalidSignatureException, AuthenticationException {

        // 1. Verify received username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUserId(), InvalidPlayerException::new);

        // 2. Verify received game id is the same from game session
        //TODO: review this exception
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameId(), AuthenticationException::new);

        // 3. Retrieve vendor line credentials and secretKey for hash validation
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);

        // 4. Verify request signature is valid
        VendorService.verifyHash(request.getRequestBody(), secretKey);
    }
}
