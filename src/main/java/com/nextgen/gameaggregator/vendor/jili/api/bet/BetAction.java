package com.nextgen.gameaggregator.vendor.jili.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jili.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.jili.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jili.service.VendorService;
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
public class BetAction {
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

    @PostMapping(path = EndPoints.BET)
    public BetVo betRequest(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        BetVo betVo = new BetVo();
        String traceId = httpRequestLog.getId();
        String vendorPlayerUsername = "";
        String vendorCurrencyCode = "";
        String token = "";

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            BetDto betDto = HttpService.convertJsonToDto(body, BetDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(betDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(betDto.getToken());
            vendorPlayerUsername = gameSession.getVendorPlayerUsername();
            vendorCurrencyCode = gameSession.getVendorCurrencyCode();
            token = gameSession.getToken();

            this.doVerification(betDto, gameSession);

            // 3. Process bet data
            // 4. Process win data
            ResultType resultType = getResultType(betDto);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, betDto, resultType, vendorService, httpRequestLog);

            betVo.setUsername(vendorPlayerUsername);
            betVo.setCurrency(vendorCurrencyCode);
            betVo.setBalance(balance);
            betVo.setToken(token);

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            betVo.setResponseCode(ResponseCode.OTHER_ERROR);
            httpService.logError(httpRequestLog, transactionStillProcessingException);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            betVo.setUsername(vendorPlayerUsername);
            betVo.setCurrency(vendorCurrencyCode);
            betVo.setBalance(betResultIdempotentViolationException.getBalance());
            betVo.setToken(token);
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);

        } catch (InvalidRequestException |
                 JsonProcessingException |
                 GameNotSupportedException |
                 CurrencyNotSupportedException invalidRequest) {
            betVo.setResponseCode(ResponseCode.INVALID_PARAMETER);
            httpService.logError(httpRequestLog, invalidRequest);

        } catch (AuthenticationException invalidSessionToken) {
            betVo.setResponseCode(ResponseCode.TOKEN_EXPIRED);
            httpService.logError(httpRequestLog, invalidSessionToken);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            betVo.setResponseCode(ResponseCode.NOT_ENOUGH_BALANCE);
            httpService.logError(httpRequestLog, insufficientBalanceException);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            //SC_INSUFFICIENT_FUNDS
            if (invalidOperatorResponseException.getOperatorStatus() == 11) {
                betVo.setResponseCode(ResponseCode.NOT_ENOUGH_BALANCE);
            } else {
                betVo.setResponseCode(ResponseCode.OTHER_ERROR);
            }
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (DisabledVendorLineException |
                 DisabledGameException |
                 DisabledAgentPlayerException |
                 BetNotFoundException |
                 InvalidAgentApiCredentialException |
                 InvalidPlayerException otherErrorException) {
            betVo.setResponseCode(ResponseCode.OTHER_ERROR);
            httpService.logError(httpRequestLog, otherErrorException);

        } catch (Exception exception) {
            betVo.setResponseCode(ResponseCode.OTHER_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, betVo);
        }
        return betVo;
    }

    private void doValidation(BetDto betDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(betDto);
    }

    private void doVerification(BetDto betDto, GameSession gameSession)
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
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(betDto.getGame()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), betDto.getCurrency(), CurrencyNotSupportedException::new);

    }

    private ResultType getResultType(BetDto dto) {

        ResultType resultType = ResultType.BET_LOSE;
        BigDecimal zero = BigDecimal.ZERO;

        if (dto.getWinloseAmount().compareTo(zero) > 0) { // Win Amount > 0 ~ BET_WIN
            resultType = ResultType.BET_WIN;
        }
        if (dto.getWinloseAmount().compareTo(zero) == 0 && dto.getBetAmount().compareTo(zero) == 0) { // Win Amount == 0 and Bet Amount == 0 ~ BET_WIN
            resultType = ResultType.BET_WIN;
        }

        return resultType;
    }
}
