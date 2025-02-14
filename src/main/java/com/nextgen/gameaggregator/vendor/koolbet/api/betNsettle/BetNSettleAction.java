package com.nextgen.gameaggregator.vendor.koolbet.api.betNsettle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkasia.service.VendorService;
import com.nextgen.gameaggregator.vendor.koolbet.api.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.koolbet.constant.EndPoints;
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
public class BetNSettleAction {

    private final HttpService httpService;

    private final GameSessionService gameSessionService;

    private final WalletService walletService;

    private final VendorService vendorService;

    private final ValidationService validationService;

    @Autowired
    public BetNSettleAction(HttpService httpService, GameSessionService gameSessionService, WalletService walletService,
                            VendorService vendorService, ValidationService validationService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.validationService = validationService;
    }

    @PostMapping(path = EndPoints.BET)
    public CommonVo bet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        CommonVo responseVo = new CommonVo();

        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into commonDto
            BetNSettleDto betNSettleDto = HttpService.convertJsonToDto(body, BetNSettleDto.class);

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(betNSettleDto);

            //get rawGameSession by token id
            GameSession gameSession = gameSessionService.verifyToken(betNSettleDto.getToken());

            //Verify remaining parameters (Verify against database values)
            this.doVerification(betNSettleDto, gameSession);

            //make a ResultType for bet and settle process indicator
            ResultType resultType = vendorService.calculateResultType(betNSettleDto.getBetAmount(), betNSettleDto.getWinAmount(), betNSettleDto.getJackpotAmount(), true);
            //Process full bet data
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, betNSettleDto, resultType, vendorService, httpRequestLog);

            //Set Response Data
            responseVo.setResponseCode(ResponseCode.BET_SUCCESS);
            responseVo.setUsername(gameSession.getVendorPlayerUsername());
            responseVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setBalance(balance.doubleValue());

            if (gameSession.getVendorPlayerUsername().equals("1e8yw13563hf")) {
                TimeUnit.SECONDS.sleep(31);
            }

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            responseVo.setResponseCode(ResponseCode.BET_OTHER_ERROR);
            httpService.logError(httpRequestLog, transactionStillProcessingException);

        } catch (BetResultIdempotentViolationException e) {
            responseVo.setResponseCode(ResponseCode.BET_ALREADY_ACCEPTED);
            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException e) {
            responseVo.setResponseCode(ResponseCode.BET_TOKEN_EXPIRED);
            httpService.logError(httpRequestLog, e);
        } catch (InsufficientBalanceException e) {
            responseVo.setResponseCode(ResponseCode.BET_INSUFFICIENT_BALANCE);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidOperatorResponseException e) {
            if (e.getOperatorStatus().equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                responseVo.setResponseCode(ResponseCode.BET_INSUFFICIENT_BALANCE);
                httpService.logError(httpRequestLog, e);
            } else {
                responseVo.setResponseCode(ResponseCode.BET_OTHER_ERROR);
                httpService.logError(httpRequestLog, e);
            }
        } catch (InvalidRequestException |
                 JsonProcessingException |
                 GameNotSupportedException |
                 CurrencyNotSupportedException e) {
            responseVo.setResponseCode(ResponseCode.BET_INVALID_PARAMETER);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.BET_OTHER_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;
    }

    private void doValidation(BetNSettleDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BetNSettleDto betNSettleDto, GameSession gameSession) throws
            AuthenticationException, CurrencyNotSupportedException, InvalidPlayerException, DisabledVendorLineException,
            DisabledAgentPlayerException, DisabledGameException, GameNotSupportedException {

        //Verify received currency is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), betNSettleDto.getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(betNSettleDto.getGame()), GameNotSupportedException::new);
        //Validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());
    }
}
