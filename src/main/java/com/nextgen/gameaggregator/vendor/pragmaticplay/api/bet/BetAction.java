package com.nextgen.gameaggregator.vendor.pragmaticplay.api.bet;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
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
public class BetAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;

    @Autowired
    private ValidationService validationService;

    @PostMapping(path = Endpoints.BET)
    public ResponseVo betRequest(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        BetVo responseVo = new BetVo();
        String traceId = httpRequestLog.getId();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            BetDto dto = HttpService.convertQueryStringToDto(body, BetDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 2. Retrieve and verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getToken());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, dto, gameSession);

            // 4. Process unsettled bet process
            BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, body);

            String transactionId = traceId.replace("-", "");
            if (betEvent.getBetInformation() != null) {
                transactionId = VendorService.getTransactionId(betEvent.getBetInformation().getBetId());
            }

            responseVo.setTransactionId(transactionId);
            responseVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setCash(betEvent.getLastBalance());
            responseVo.setBonus(BigDecimal.ZERO);
            responseVo.setUsedPromo(BigDecimal.ZERO);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);
            if (invalidRequestException.getValidation() != null) {
                httpRequestLog.setErrorMessage(invalidRequestException.getValidation().toString());
            }

        } catch (CredentialNotFoundException credentialNotFoundException) {
            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);

        } catch (InvalidPlayerException invalidPlayerException) {
            responseVo.setResponseCode(ResponseCode.PLAYER_NOT_FOUND);

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            responseVo.setResponseCode(ResponseCode.PLAYER_FROZEN);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            responseVo.setResponseCode(ResponseCode.BET_NOT_ALLOWED);

        } catch (AuthenticationException authenticationException) {
            responseVo.setResponseCode(ResponseCode.AUTHENTICATION_ERROR);

        } catch (InvalidSignatureException invalidHashException) {
            responseVo.setResponseCode(ResponseCode.INVALID_HASH);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            responseVo.setResponseCode(ResponseCode.INSUFFICIENT_BALANCE);

        } catch (DisabledVendorLineException disabledVendorLineException) {
            responseVo.setResponseCode(ResponseCode.BET_NOT_ALLOWED);

        } catch (DisabledGameException disabledGameException) {
            responseVo.setResponseCode(ResponseCode.INVALID_GAME);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_RETRY);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (Exception exception) { // any other exception encountered
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_NO_RETRY);
            httpService.logError(httpRequestLog, exception);
        }

        httpService.end(httpRequestLog, responseVo);
        return responseVo;
    }

    private void doValidation(BetDto dto) throws InvalidRequestException, InvalidPlayerException {
        // General validation
        ValidationUtils.validateRequest(dto);
        // Validation with custom exception
        ValidationUtils.validateLength(dto.getUserId(), 3, 20, InvalidPlayerException::new);
        ValidationUtils.isEquals(dto.getProviderId(), Credentials.PROVIDER_ID);
    }

    private void doVerification(HttpRequestLog request, BetDto dto, GameSession gameSession) throws
            AuthenticationException, InvalidPlayerException, CredentialNotFoundException,
            InvalidSignatureException, DisabledVendorLineException, DisabledAgentPlayerException,
            DisabledGameException {

        //1. validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getUserId());

        // 2. Verify received game id is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        if (!gameSession.getVendorGameCode().equals("101")) {
            ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameId(), AuthenticationException::new);
        }


        // 4. Retrieve vendor line credentials and secretKey for hash validation
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);

        // 5. Verify request signature is valid
        VendorService.verifyHash(request.getRequestBody(), secretKey);
    }
}
