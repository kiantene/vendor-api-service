package com.nextgen.gameaggregator.vendor.koolbet.api.sessionBetNsettle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkasia.service.VendorService;
import com.nextgen.gameaggregator.vendor.koolbet.api.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.koolbet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.koolbet.constant.Formats;
import com.nextgen.gameaggregator.vendor.koolbet.constant.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class SessionBetNSettleAction {

    private final HttpService httpService;

    private final GameSessionService gameSessionService;

    private final WalletService walletService;

    private final VendorService vendorService;


    private final ValidationService validationService;

    @Autowired
    public SessionBetNSettleAction(HttpService httpService, GameSessionService gameSessionService,
                                   WalletService walletService, VendorService vendorService,
                                   ValidationService validationService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.validationService = validationService;
    }

    @PostMapping(path = EndPoints.SESSION_BET)
    public CommonVo bet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        CommonVo responseVo = new CommonVo();

        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into commonDto
            SessionBetNSettleDto sessionBetNSettleDto = HttpService.convertJsonToDto(body, SessionBetNSettleDto.class);

            //Validate request parameters from vendor (Non-database related)
            //this.doValidation(commonDto);

            //get rawGameSession by token id
            GameSession gameSession = gameSessionService.verifyToken(sessionBetNSettleDto.getToken());

            //Verify remaining parameters (Verify against database values)
            this.doVerification(sessionBetNSettleDto, gameSession);

            switch (sessionBetNSettleDto.getType()) {
                case Formats.SESSION_BET_TYPE_BET -> {
                    BetEvent betEvent = walletService.processBet(traceId, gameSession, sessionBetNSettleDto,
                            body, httpRequestLog);
                    responseVo.setBalance(betEvent.getLastBalance().doubleValue());
                }
                case Formats.SESSION_BET_TYPE_SETTLE -> {
                    //make a ResultType for bet and settle process indicator
                    ResultType resultType = vendorService.calculateResultType(sessionBetNSettleDto.getBetAmount(),
                            sessionBetNSettleDto.getWinAmount(), sessionBetNSettleDto.getJackpotAmount(), false);

                    BigDecimal balance = walletService.processBetResult(traceId, gameSession, sessionBetNSettleDto,
                            resultType, vendorService, httpRequestLog);
                    responseVo.setBalance(balance.doubleValue());
                }
                default -> throw new InvalidRequestException();
            }

            //Set Response Data
            responseVo.setResponseCode(ResponseCode.SESSION_BET_SUCCESS);
            responseVo.setUsername(gameSession.getVendorPlayerUsername());
            responseVo.setCurrency(gameSession.getVendorCurrencyCode());

            if (gameSession.getVendorPlayerUsername().equals("1e8yw13563gf")) {
                TimeUnit.SECONDS.sleep(31);
            }

        } catch (BetResultIdempotentViolationException e) {
            responseVo.setResponseCode(ResponseCode.SESSION_BET_ALREADY_ACCEPTED);
            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException e) {
            responseVo.setResponseCode(ResponseCode.SESSION_BET_TOKEN_EXPIRED);
            httpService.logError(httpRequestLog, e);
        } catch (InsufficientBalanceException e) {
            responseVo.setResponseCode(ResponseCode.SESSION_BET_INSUFFICIENT_BALANCE);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidRequestException |
                 JsonProcessingException |
                 GameNotSupportedException |
                 CurrencyNotSupportedException e) {
            responseVo.setResponseCode(ResponseCode.SESSION_BET_INVALID_PARAMETER);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.SESSION_BET_OTHER_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;
    }

    private void doVerification(SessionBetNSettleDto sessionBetNSettleDto, GameSession gameSession) throws
            AuthenticationException, InvalidRequestException, CurrencyNotSupportedException, InvalidPlayerException,
            DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, GameNotSupportedException {

        //Verify received currency is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(sessionBetNSettleDto.getGame()),
                GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(),
                sessionBetNSettleDto.getCurrency(), CurrencyNotSupportedException::new);

        if (sessionBetNSettleDto.getType().equals(Formats.SESSION_BET_TYPE_BET)) {
            //Validate vendor username, agent vendor line, player status, and game status
            validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());
        }
    }
}
