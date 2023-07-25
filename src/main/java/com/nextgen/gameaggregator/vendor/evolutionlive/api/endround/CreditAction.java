package com.nextgen.gameaggregator.vendor.evolutionlive.api.endround;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.evolutionlive.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.evolutionlive.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.evolutionlive.service.VendorService;
import com.nextgen.gameaggregator.vendor.evolutionlive.vo.ResponseVo;
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
public class CreditAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.CREDIT)
    public ResponseVo creditAction(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        ResponseVo responseVo = new ResponseVo();
        String traceId = httpRequestLog.getId();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            CreditDto creditDto = HttpService.convertJsonToDto(body, CreditDto.class);


            // 1. Validate request parameters (Non-database calls)
            this.doValidation(creditDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(creditDto.getSid());

            this.doVerification(creditDto, gameSession);

            // 3.
            ResultType resultType = vendorService.calculateResultType(creditDto.getBetAmount(), creditDto.getWinAmount(), creditDto.getJackpotAmount(), false);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, creditDto, resultType, vendorService, httpRequestLog);

            responseVo.setBalance(balance);
            responseVo.setUuid(creditDto.getUuid());


        } catch (AuthenticationException e) {
            responseVo.setResponseCode(ResponseCode.INVALID_SID);
            httpService.logError(httpRequestLog, e);
        } catch (JsonProcessingException |
                 InvalidRequestException |
                 GameNotSupportedException |
                 InvalidPlayerException |
                 CurrencyNotSupportedException e) {
            responseVo.setResponseCode(ResponseCode.INVALID_PARAMETER);
            httpService.logError(httpRequestLog, e);
        } catch (DisabledVendorLineException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 InvalidOperatorResponseException |
                 TransactionStillProcessingException |
                 InvalidAgentApiCredentialException e) {
            responseVo.setResponseCode(ResponseCode.TEMPORARY_ERROR);
            httpService.logError(httpRequestLog, e);
        } catch (InsufficientBalanceException e) {
            responseVo.setResponseCode(ResponseCode.INSUFFICIENT_FUNDS);
            httpService.logError(httpRequestLog, e);
        } catch (BetNotFoundException e) {
            responseVo.setResponseCode(ResponseCode.BET_DOES_NOT_EXIST);
            httpService.logError(httpRequestLog, e);
        } catch (BetResultIdempotentViolationException e) {
            idempotentSetBalance(httpRequestLog, responseVo);
        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;

    }

    private void doValidation(CreditDto creditDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(creditDto);
        ValidationUtils.validateRequest(creditDto.getGame());
        ValidationUtils.validateRequest(creditDto.getTransaction());
    }

    private void doVerification(CreditDto creditDto, GameSession gameSession)
            throws
            AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            GameNotSupportedException,
            CurrencyNotSupportedException,
            InvalidPlayerException {

        // 1. Verify Username, GameCode, CurrencyCode
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), creditDto.getUserId(), InvalidPlayerException::new);
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(creditDto.getGame().getDetails().getTable().getId()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), creditDto.getCurrency(), CurrencyNotSupportedException::new);
    }

    private void idempotentSetBalance(HttpRequestLog httpRequestLog, ResponseVo responseVo) {
        try {
            CreditDto creditDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), CreditDto.class);
            GameSession gameSession = gameSessionService.verifyToken(creditDto.getSid());
            responseVo.setBalance(walletService.getBalance(httpRequestLog.getId(), gameSession));
            responseVo.setUuid(creditDto.getUuid());
        } catch (InvalidOperatorResponseException e) {
            responseVo.setResponseCode(ResponseCode.TEMPORARY_ERROR);
        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.UNKNOWN_ERROR);
        }
    }
}
