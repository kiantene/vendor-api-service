package com.nextgen.gameaggregator.vendor.jili.api.cancelbet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
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
    private VendorLineService vendorLineService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private BetHistoryService betHistoryService;
    @Autowired
    private VendorService vendorService;

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
            cancelBetVo.setResponseCode(ResponseCode.OTHER_ERROR);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            if (invalidOperatorResponseException.getOperatorStatus() == 11) {
                //insufficient balance
                cancelBetVo.setResponseCode(ResponseCode.ALREADY_ACCEPTED_AND_CANNOT_BE_CANCELED);

            } else if (invalidOperatorResponseException.getOperatorStatus() == 15) {
                //Operator Bet not found
                cancelBetVo.setResponseCode(ResponseCode.ROUND_NOT_FOUND);

            } else {
                //Other operator errors
                cancelBetVo.setResponseCode(ResponseCode.OTHER_ERROR);

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
                 InvalidAgentApiCredentialException e) {
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
            CurrencyNotSupportedException {

        // 1. Verify received token is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getToken(), cancelBetDto.getToken(), AuthenticationException::new);

        // Verify vendor gameCode and currency
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(cancelBetDto.getGame()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), cancelBetDto.getCurrency(), CurrencyNotSupportedException::new);

        // 2. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 3. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 4. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

    }
}
