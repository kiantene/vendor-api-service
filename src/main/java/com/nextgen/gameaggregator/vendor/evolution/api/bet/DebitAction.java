package com.nextgen.gameaggregator.vendor.evolution.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
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

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class DebitAction {
    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final ValidationService validationService;
    private final VendorService vendorService;

    @Autowired
    public DebitAction(HttpService httpService, GameSessionService gameSessionService, WalletService walletService, ValidationService validationService, VendorService vendorService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.validationService = validationService;
        this.vendorService = vendorService;
    }

    @PostMapping(path = EndPoints.DEBIT)
    public ResponseVo DebitAction(HttpServletRequest request) throws JsonProcessingException {
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
            GameSession gameSession = vendorService.preCheckGameSessionToken(debitDto.getSid());

            this.doVerification(debitDto, gameSession);

            // process bet
            BetEvent betEvent = walletService.processBet(traceId, gameSession, debitDto, body, httpRequestLog);

            responseVo.setBalance(betEvent.getLastBalance());
            responseVo.setUuid(debitDto.getUuid());


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

        } catch (BetResultIdempotentViolationException e) {
            idempotentSetBalance(httpRequestLog, responseVo);

        } catch (DuplicateExternalTransactionIdException e) {
            responseVo.setResponseCode(ResponseCode.FINAL_ERROR_ACTION_FAILED);

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
            InvalidPlayerException,
            DuplicateExternalTransactionIdException {

        // 1. Verify received token is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getVendorToken(), debitDto.getSid(), AuthenticationException::new);
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), debitDto.getUserId(), InvalidPlayerException::new);
        // Verify vendor gameCode and currency
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(debitDto.getGame().getDetails().getTable().getId()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), debitDto.getCurrency(), CurrencyNotSupportedException::new);

        // 2. validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, debitDto.getUserId());

        // Verify debit after rollback or not
        vendorService.verifyDebitAfterRollback(gameSession.getVendorPlayerId(), debitDto.getExternalTransactionId());
    }

    private void idempotentSetBalance(HttpRequestLog httpRequestLog, ResponseVo responseVo) {
        try {
            DebitDto debitDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), DebitDto.class);
            GameSession gameSession = vendorService.preCheckGameSessionToken(debitDto.getSid());
            responseVo.setBalance(walletService.getBalance(httpRequestLog.getId(), gameSession, httpRequestLog));
            responseVo.setUuid(debitDto.getUuid());
        } catch (InvalidOperatorResponseException e) {
            responseVo.setResponseCode(ResponseCode.TEMPORARY_ERROR);
        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.UNKNOWN_ERROR);
        }
    }
}
