package com.nextgen.gameaggregator.vendor.pragmaticplay.api.jackpot;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetResultEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pragmaticplay.service.VendorService;
import com.nextgen.gameaggregator.vendor.pragmaticplay.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

@RestController
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
public class JackpotAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @PostMapping(path = Endpoints.JACKPOT)
    public ResponseVo jackpot(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        JackpotVo responseVo = new JackpotVo();
        String traceId = httpRequestLog.getTraceId();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            JackpotDto dto = HttpService.convertQueryStringToDto(body, JackpotDto.class);

            // 1. Validate request parameters from vendor
            this.doValidation(dto);

            // TODO: validate gameId with gameSession

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getToken());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, dto, gameSession);

            // 5. Send win result to Operator
            BetResultEvent betResultEvent = walletService.processWin(traceId, gameSession, dto, body);

            responseVo.setTransactionId(traceId);
            responseVo.setCurrency(gameSession.getVendorGameCode());
            responseVo.setCash(betResultEvent.getLastBalance());
            responseVo.setBonus(BigDecimal.ZERO);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);
            if (invalidRequestException.getValidation() != null) {
                httpRequestLog.setErrorMessage(invalidRequestException.getValidation().toString());
            }

        } catch (InvalidPlayerException invalidPlayerException) {
            responseVo.setResponseCode(ResponseCode.PLAYER_NOT_FOUND);

        } catch (AuthenticationException authenticationException) {
            responseVo.setResponseCode(ResponseCode.AUTHENTICATION_ERROR);

        } catch (InvalidSignatureException invalidSignatureException) {
            responseVo.setResponseCode(ResponseCode.INVALID_HASH);

        } catch (DuplicateExternalTransactionIdException duplicateExternalTransactionIdException) {
            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);
            httpService.logError(httpRequestLog, duplicateExternalTransactionIdException);

        } catch (BetNotFoundException betNotFoundException) {
            responseVo.setResponseCode(ResponseCode.BET_NOT_ALLOWED);
            httpService.logError(httpRequestLog, betNotFoundException);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_RETRY);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (CredentialNotFoundException credentialNotFoundException) {
            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);
            
        } catch (Exception exception) { // any other exception encountered
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_NO_RETRY);
            httpService.logError(httpRequestLog, exception);
        }

        httpService.end(httpRequestLog, responseVo);
        return responseVo;
    }

    private void doValidation(JackpotDto dto) throws InvalidRequestException, InvalidPlayerException {
        // General validation
        ValidationUtils.validateRequest(dto);
        // Validation with custom exception
        ValidationUtils.validateLength(dto.getUserId(), 3, 20, InvalidPlayerException::new);
        ValidationUtils.isEquals(dto.getProviderId(), Credentials.PROVIDER_ID);
    }

    private void doVerification(HttpRequestLog request, JackpotDto dto, GameSession gameSession) throws
            InvalidPlayerException, AuthenticationException, CredentialNotFoundException, InvalidSignatureException {

        // 1. Verify received username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUserId(), InvalidPlayerException::new);

        // 2. Verify received game id is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameId(), AuthenticationException::new);

        // 3. Retrieve vendor line credentials and secretKey for hash validation
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);

        // 4. Verify request signature is valid
        VendorService.verifyHash(request.getRequestBody(), secretKey);
    }

}
