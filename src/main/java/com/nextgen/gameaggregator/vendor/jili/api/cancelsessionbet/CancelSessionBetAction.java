package com.nextgen.gameaggregator.vendor.jili.api.cancelsessionbet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jili.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.jili.constant.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CancelSessionBetAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private ValidationService validationService;


    @PostMapping(path = EndPoints.CANCEL_SESSION_BET)
    public CancelSessionBetVo CancelSessionBetAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        CancelSessionBetVo cancelSessionBetVo = new CancelSessionBetVo();
        String traceId = httpRequestLog.getId();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            CancelSessionBetDto cancelSessionBetDto = HttpService.convertJsonToDto(body, CancelSessionBetDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(cancelSessionBetDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(cancelSessionBetDto.getToken());

            // 3. Verify request parameters
            this.doVerification(cancelSessionBetDto, gameSession);

            // 4. Send refund to Operator
            BigDecimal balance = walletService.processRollback(traceId, cancelSessionBetDto, gameSession, vendorService, httpRequestLog);

            cancelSessionBetVo.setUsername(gameSession.getVendorPlayerUsername());
            cancelSessionBetVo.setCurrency(gameSession.getVendorCurrencyCode());
            cancelSessionBetVo.setBalance(balance);


        } catch (BetNotFoundException betNotFoundException) {
            cancelSessionBetVo.setResponseCode(ResponseCode.ROUND_NOT_FOUND);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            if (betResultIdempotentViolationException.getStatus().equals(BetStatus.SETTLED.code)) {
                //if found the bet in settled status
                cancelSessionBetVo.setResponseCode(ResponseCode.ALREADY_ACCEPTED_AND_CANNOT_BE_CANCELED);

            }
            if (betResultIdempotentViolationException.getStatus().equals(BetStatus.REFUNDED.code)) {
                //if found the bet in refunded status
                cancelSessionBetVo.setResponseCode(ResponseCode.ALREADY_ACCEPTED);

            } else {
                //if found the bet other in settled status (cancel)
                cancelSessionBetVo.setResponseCode(ResponseCode.OTHER_ERROR);

            }

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            cancelSessionBetVo.setResponseCode(ResponseCode.OTHER_ERROR);
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            if (invalidOperatorResponseException.getOperatorStatus().equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                //insufficient balance
                cancelSessionBetVo.setResponseCode(ResponseCode.ALREADY_ACCEPTED_AND_CANNOT_BE_CANCELED);

            } else if (invalidOperatorResponseException.getOperatorStatus().equals(ResponseCodes.Status.SC_TRANSACTION_NOT_EXISTS.code)) {
                //Operator Bet not found
                cancelSessionBetVo.setResponseCode(ResponseCode.ROUND_NOT_FOUND);

            } else {
                //If other operator errors set code -1 error
                // cancelSessionBetVo.setResponseCode(ResponseCode.OTHER_ERROR);
                cancelSessionBetVo.setResponseCode(ResponseCode.ALREADY_ACCEPTED);
            }
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (InvalidRequestException |
                 JsonProcessingException |
                 GameNotSupportedException |
                 CurrencyNotSupportedException invalidRequest) {
            cancelSessionBetVo.setResponseCode(ResponseCode.INVALID_PARAMETER);

        } catch (AuthenticationException invalidSessionToken) {
            cancelSessionBetVo.setResponseCode(ResponseCode.TOKEN_EXPIRED);

        } catch (DisabledVendorLineException |
                 DisabledGameException |
                 DisabledAgentPlayerException |
                 InvalidAgentApiCredentialException |
                 InvalidPlayerException otherErrorException) {
            cancelSessionBetVo.setResponseCode(ResponseCode.OTHER_ERROR);

        } catch (Exception exception) {
            cancelSessionBetVo.setResponseCode(ResponseCode.OTHER_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, cancelSessionBetVo);
        }
        return cancelSessionBetVo;
    }

    private void doValidation(CancelSessionBetDto cancelSessionBetDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(cancelSessionBetDto);
    }

    private void doVerification(CancelSessionBetDto cancelSessionBetDto, GameSession gameSession)
            throws
            AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            GameNotSupportedException,
            CurrencyNotSupportedException,
            InvalidPlayerException {

        // validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());

        // Verify vendor gameCode and currency
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(cancelSessionBetDto.getGame()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), cancelSessionBetDto.getCurrency(), CurrencyNotSupportedException::new);

    }
}
