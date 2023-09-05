package com.nextgen.gameaggregator.vendor.jili.api.cancelbet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cq9.service.VendorService;
import com.nextgen.gameaggregator.vendor.jili.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.jili.constant.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CancelBetAction {
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

    @PostMapping(path = EndPoints.CANCEL_BET)
    public CancelBetVo CancelBetAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        CancelBetVo cancelBetVo = new CancelBetVo();
        String traceId = httpRequestLog.getId();
        String vendorPlayerUsername = null;
        String vendorCurrencyCode = null;

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            CancelBetDto cancelBetDto = HttpService.convertJsonToDto(body, CancelBetDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(cancelBetDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(cancelBetDto.getToken());
            vendorPlayerUsername = gameSession.getVendorPlayerUsername();
            vendorCurrencyCode = gameSession.getVendorCurrencyCode();

            // 3. get Bet History for checking
            this.doVerification(cancelBetDto, gameSession);

            // 4. Send refund to Operator
            BigDecimal balance = walletService.processRollback(traceId, cancelBetDto, gameSession, vendorService);

            cancelBetVo.setUsername(vendorPlayerUsername);
            cancelBetVo.setCurrency(vendorCurrencyCode);
            cancelBetVo.setBalance(balance);

        } catch (BetNotFoundException betNotFoundException) {
            cancelBetVo.setResponseCode(ResponseCode.ROUND_NOT_FOUND);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            if (betResultIdempotentViolationException.getStatus() == BetStatus.SETTLED.code) {
                //if found the bet in settled status
                cancelBetVo.setResponseCode(ResponseCode.ALREADY_ACCEPTED_AND_CANNOT_BE_CANCELED);

            } else {
                //if found the bet other in settled status (cancel / refund)
                cancelBetVo.setUsername(vendorPlayerUsername);
                cancelBetVo.setCurrency(vendorCurrencyCode);
                cancelBetVo.setBalance(betResultIdempotentViolationException.getBalance());

            }

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            cancelBetVo.setResponseCode(ResponseCode.ALREADY_ACCEPTED);
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            if (invalidOperatorResponseException.getOperatorStatus().equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                //insufficient balance
                cancelBetVo.setResponseCode(ResponseCode.ALREADY_ACCEPTED_AND_CANNOT_BE_CANCELED);

            } else if (invalidOperatorResponseException.getOperatorStatus().equals(ResponseCodes.Status.SC_TRANSACTION_NOT_EXISTS.code)) {
                //Operator Bet not found
                cancelBetVo.setResponseCode(ResponseCode.ROUND_NOT_FOUND);

            } else {
                //If other operator errors set code -1 error
                // cancelBetVo.setResponseCode(ResponseCode.OTHER_ERROR);
                cancelBetVo.setResponseCode(ResponseCode.ALREADY_ACCEPTED);
            }
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (InvalidRequestException |
                 JsonProcessingException |
                 GameNotSupportedException |
                 CurrencyNotSupportedException invalidRequest) {
            cancelBetVo.setResponseCode(ResponseCode.INVALID_PARAMETER);

        } catch (AuthenticationException invalidSessionToken) {
            cancelBetVo.setResponseCode(ResponseCode.TOKEN_EXPIRED);

        } catch (DisabledVendorLineException |
                 DisabledGameException |
                 DisabledAgentPlayerException |
                 InvalidAgentApiCredentialException |
                 InvalidPlayerException otherErrorException) {
            cancelBetVo.setResponseCode(ResponseCode.OTHER_ERROR);

        } catch (Exception exception) {
            cancelBetVo.setResponseCode(ResponseCode.OTHER_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, cancelBetVo);
        }
        return cancelBetVo;
    }

    private void doValidation(CancelBetDto cancelBetDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(cancelBetDto);
    }

    private void doVerification(CancelBetDto cancelBetDto, GameSession gameSession)
            throws
            AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            GameNotSupportedException,
            CurrencyNotSupportedException,
            InvalidPlayerException {

        // Verify vendor gameCode and currency
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(cancelBetDto.getGame()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), cancelBetDto.getCurrency(), CurrencyNotSupportedException::new);

    }
}
