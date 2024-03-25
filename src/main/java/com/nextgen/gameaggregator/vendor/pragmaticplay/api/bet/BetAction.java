package com.nextgen.gameaggregator.vendor.pragmaticplay.api.bet;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pragmaticplay.service.VendorService;
import com.nextgen.gameaggregator.vendor.pragmaticplay.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = Endpoints.BET)
    public ResponseVo betRequest(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        BetVo responseVo = new BetVo();
        String traceId = httpRequestLog.getId();
        String vendorCurrencyCode = "";
        GameSession gameSession = new GameSession();

        try {
            // Retrieve request body in original string format and convert into dto
            BetDto dto = HttpService.convertQueryStringToDto(httpRequestLog, BetDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 2. Retrieve and verify session token
            gameSession = gameSessionService.verifyToken(dto.getToken());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(dto.getGameId(), gameSession);
            vendorCurrencyCode = gameSession.getVendorCurrencyCode();

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, dto, gameSession);

            // 4. Process unsettled bet process
            BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, httpRequestLog.getRequestBody(), httpRequestLog);

            String transactionId = VendorService.getTransactionId(traceId);
            responseVo.setTransactionId(transactionId);
            responseVo.setCurrency(vendorCurrencyCode);
            responseVo.setCash(betEvent.getLastBalance());
            responseVo.setBonus(BigDecimal.ZERO);
            responseVo.setUsedPromo(BigDecimal.ZERO);

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_RETRY);
            httpService.logError(httpRequestLog, transactionStillProcessingException);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            String betId = betResultIdempotentViolationException.getBetId();
            responseVo.setTransactionId(VendorService.getTransactionId(betId));
            responseVo.setCurrency(vendorCurrencyCode);
            responseVo.setCash(vendorService.getCurrentBalance(traceId, gameSession, httpRequestLog));
            responseVo.setBonus(BigDecimal.ZERO);
            responseVo.setUsedPromo(BigDecimal.ZERO);
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);
            if (invalidRequestException.getValidation() != null) {
                httpRequestLog.setErrorMessage(invalidRequestException.getValidation().toString());
            }
            httpService.logError(httpRequestLog, invalidRequestException);

        } catch (CredentialNotFoundException credentialNotFoundException) {
            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);
            httpService.logError(httpRequestLog, credentialNotFoundException);

        } catch (InvalidPlayerException invalidPlayerException) {
            responseVo.setResponseCode(ResponseCode.PLAYER_NOT_FOUND);
            httpService.logError(httpRequestLog, invalidPlayerException);

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            responseVo.setResponseCode(ResponseCode.PLAYER_FROZEN);
            httpService.logError(httpRequestLog, disabledAgentPlayerException);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            responseVo.setResponseCode(ResponseCode.BET_NOT_ALLOWED);
            httpService.logError(httpRequestLog, invalidAgentApiCredentialException);

        } catch (AuthenticationException authenticationException) {
            responseVo.setResponseCode(ResponseCode.AUTHENTICATION_ERROR);
            httpService.logError(httpRequestLog, authenticationException);

        } catch (InvalidSignatureException invalidHashException) {
            responseVo.setResponseCode(ResponseCode.INVALID_HASH);
            httpService.logError(httpRequestLog, invalidHashException);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            responseVo.setResponseCode(ResponseCode.INSUFFICIENT_BALANCE);
            httpService.logError(httpRequestLog, insufficientBalanceException);

        } catch (DisabledVendorLineException disabledVendorLineException) {
            responseVo.setResponseCode(ResponseCode.BET_NOT_ALLOWED);
            httpService.logError(httpRequestLog, disabledVendorLineException);

        } catch (DisabledGameException | GameNotSupportedException disabledGameException) {
            responseVo.setResponseCode(ResponseCode.INVALID_GAME);
            httpService.logError(httpRequestLog, disabledGameException);

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
            DisabledGameException, GameNotSupportedException {

        // 1. validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getUserId());

        // 2. Retrieve vendor line credentials and secretKey for hash validation
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);

        // 3. Verify request signature is valid
        VendorService.verifyHash(request.getRequestBody(), secretKey);

        // 4. not needed to check is game availability, because validateEligibleBet already done the checking
    }
}
