package com.nextgen.gameaggregator.vendor.evolutionlive.api.endround;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
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
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CreditAction {
    private final HttpService httpService;
    private final WalletService walletService;
    private final AutowireCapableBeanFactory autowireCapableBeanFactory;
    private final VendorService vendorService;
    private final RequestIdempotentLogService requestIdempotentLogService;
    private final GameSessionService gameSessionService;

    @Autowired
    public CreditAction(HttpService httpService, WalletService walletService,
                        AutowireCapableBeanFactory autowireCapableBeanFactory,
                        VendorService vendorService,
                        RequestIdempotentLogService requestIdempotentLogService,
                        GameSessionService gameSessionService) {
        this.httpService = httpService;
        this.walletService = walletService;
        this.autowireCapableBeanFactory = autowireCapableBeanFactory;
        this.vendorService = vendorService;
        this.requestIdempotentLogService = requestIdempotentLogService;
        this.gameSessionService = gameSessionService;
    }

    @PostMapping(path = EndPoints.CREDIT)
    public ResponseVo creditAction(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        ResponseVo responseVo = new ResponseVo();
        String traceId = httpRequestLog.getId();
        CreditDto creditDto = new CreditDto();
        GameSession gameSession;
        boolean isRequestExists = false;

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            creditDto = HttpService.convertJsonToDto(body, CreditDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(creditDto);
            String vendorGameCode = creditDto.getGame().getDetails().getTable().getId();

            if (requestIdempotentLogService.checkExists(creditDto, creditDto.getUserId()) == null) {
                requestIdempotentLogService.create(creditDto, creditDto.getUserId());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            VendorService vendorService = new VendorService();
            autowireCapableBeanFactory.autowireBean(vendorService);

            // 2. Verify session token
            try {
                gameSession = vendorService.preCheckGameSessionToken(creditDto.getSid());
                gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(vendorGameCode, gameSession);
            } catch (AuthenticationException e) {
                gameSession = gameSessionService.generateNewSessionToken(creditDto.getUserId());
                gameSessionService.updateByVendorGameCode(gameSession, vendorGameCode);
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }
            this.doVerification(creditDto, gameSession);

            // 3.
            ResultType resultType = vendorService.calculateResultType(creditDto.getBetAmount(), creditDto.getWinAmount(), creditDto.getJackpotAmount(), false);
            vendorService.verifyIsPreProcessingVendorGame(gameSession.getVendorGameId());
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
            httpService.logError(httpRequestLog, e);
            idempotentSetBalance(httpRequestLog, responseVo);
            responseVo.setResponseCode(ResponseCode.BET_ALREADY_SETTLED);
        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            if (!isRequestExists) {
                requestIdempotentLogService.delete(creditDto, creditDto.getUserId());
            }
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
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), creditDto.getCurrency(), CurrencyNotSupportedException::new);
    }

    private void idempotentSetBalance(HttpRequestLog httpRequestLog, ResponseVo responseVo) {
        try {
            CreditDto creditDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), CreditDto.class);
            GameSession gameSession = vendorService.preCheckGameSessionToken(creditDto.getSid());
            responseVo.setBalance(walletService.getBalance(httpRequestLog.getId(), gameSession, httpRequestLog));
            responseVo.setUuid(creditDto.getUuid());
        } catch (InvalidOperatorResponseException e) {
            responseVo.setResponseCode(ResponseCode.TEMPORARY_ERROR);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, e);
        }
    }
}

