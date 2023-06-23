package com.nextgen.gameaggregator.vendor.evolutionlive.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.evolutionlive.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.evolutionlive.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.evolutionlive.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class DebitAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;

    @PostMapping(path = EndPoints.DEBIT)
    public ResponseVo debitAction(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        ResponseVo responseVo = new ResponseVo();
        String traceId = httpRequestLog.getId();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            DebitDto debitDto = HttpService.convertJsonToDto(body, DebitDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(debitDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(debitDto.getSid());

            this.doVerification(debitDto, gameSession);

            // process bet
            BetEvent betEvent = walletService.processBet(traceId, gameSession, debitDto, body);

            responseVo.setBalance(betEvent.getLastBalance());
            responseVo.setUuid(debitDto.getUuid());


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
                 InvalidAgentApiCredentialException e) {
            responseVo.setResponseCode(ResponseCode.TEMPORARY_ERROR);
            httpService.logError(httpRequestLog, e);
        } catch (InsufficientBalanceException e) {
            responseVo.setResponseCode(ResponseCode.INSUFFICIENT_FUNDS);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;

    }

    private void doValidation(DebitDto debitDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(debitDto);
        ValidationUtils.validateRequest(debitDto.getGame());
        ValidationUtils.validateRequest(debitDto.getTransaction());
    }

    private void doVerification(DebitDto debitDto, GameSession gameSession)
            throws
            AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            GameNotSupportedException,
            CurrencyNotSupportedException,
            InvalidPlayerException {

        // 1. Verify Username, GameCode, CurrencyCode
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), debitDto.getUserId(), InvalidPlayerException::new);
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(debitDto.getGame().getDetails().getTable().getId()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), debitDto.getCurrency(), CurrencyNotSupportedException::new);

        // 2. validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, debitDto.getUserId());
    }
}
