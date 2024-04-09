package com.nextgen.gameaggregator.vendor.evolution.api.endround;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.evolution.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.evolution.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.evolution.service.VendorService;
import com.nextgen.gameaggregator.vendor.evolution.vo.ResponseVo;
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
    public ResponseVo CreditAction(HttpServletRequest request) {
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
            ResultType resultType = (creditDto.getWinAmount().compareTo(BigDecimal.ZERO) > 0) ? ResultType.WIN : ResultType.END;
            vendorService.verifyIsPreProcessingVendorGame(gameSession.getVendorGameId());
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, creditDto, resultType, vendorService, httpRequestLog);

            responseVo.setBalance(balance);
            responseVo.setUuid(creditDto.getUuid());


        } catch (AuthenticationException e) {
            responseVo.setResponseCode(ResponseCode.INVALID_SID);

        } catch (JsonProcessingException |
                 InvalidRequestException |
                 GameNotSupportedException |
                 InvalidPlayerException |
                 CurrencyNotSupportedException e) {
            responseVo.setResponseCode(ResponseCode.INVALID_PARAMETER);

        } catch (DisabledVendorLineException |
                 DisabledGameException |
                 InvalidOperatorResponseException |
                 InvalidAgentApiCredentialException |
                 TransactionStillProcessingException e) {
            responseVo.setResponseCode(ResponseCode.TEMPORARY_ERROR);

        } catch (DisabledAgentPlayerException e) {
            responseVo.setResponseCode(ResponseCode.ACCOUNT_LOCKED);

        } catch (InsufficientBalanceException e) {
            responseVo.setResponseCode(ResponseCode.INSUFFICIENT_FUNDS);

        } catch (BetNotFoundException e) {
            responseVo.setResponseCode(ResponseCode.BET_DOES_NOT_EXIST);

        } catch (BetResultIdempotentViolationException e) {
            idempotentSetBalance(httpRequestLog, responseVo);
            responseVo.setResponseCode(ResponseCode.BET_ALREADY_SETTLED);

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
            InvalidPlayerException, BetNotFoundException {

        // 1. Verify received token is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getToken(), creditDto.getSid(), AuthenticationException::new);
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), creditDto.getUserId(), InvalidPlayerException::new);
        // Verify vendor gameCode and currency
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(creditDto.getGame().getDetails().getTable().getId()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), creditDto.getCurrency(), CurrencyNotSupportedException::new);

    }

    private void idempotentSetBalance(HttpRequestLog httpRequestLog, ResponseVo responseVo) {
        try {
            CreditDto creditDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), CreditDto.class);
            GameSession gameSession = gameSessionService.verifyToken(creditDto.getSid());
            responseVo.setBalance(walletService.getBalance(httpRequestLog.getId(), gameSession, httpRequestLog));
            responseVo.setUuid(creditDto.getUuid());
        } catch (InvalidOperatorResponseException e) {
            responseVo.setResponseCode(ResponseCode.TEMPORARY_ERROR);
        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.UNKNOWN_ERROR);
        }
    }
}
