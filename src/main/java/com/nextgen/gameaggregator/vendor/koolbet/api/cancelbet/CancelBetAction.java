package com.nextgen.gameaggregator.vendor.koolbet.api.cancelbet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.koolbet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.koolbet.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.koolbet.service.VendorService;
import com.nextgen.gameaggregator.vendor.koolbet.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CancelBetAction {

    private final HttpService httpService;

    private final GameSessionService gameSessionService;

    private final WalletService walletService;

    private final VendorService vendorService;


    @Autowired
    public CancelBetAction(HttpService httpService, GameSessionService gameSessionService, WalletService walletService
            , VendorService vendorService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;

    }

    @PostMapping(path = EndPoints.CANCEL_BET)
    public CommonVo rollback(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        CommonVo responseVo = new CommonVo();

        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            CancelBetDto dto = HttpService.convertJsonToDto(body, CancelBetDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 3. Verify session token
            GameSession gameSession;
            try {
                gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getUserId());
            } catch (AuthenticationException authenticationException) { //if session expired
                gameSession = gameSessionService.generateNewSessionToken(dto.getUserId()); //generate new token
                gameSessionService.updateByVendorGameCode(gameSession, String.valueOf(dto.getGame()));
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);

            // 5. Process rollback
            BigDecimal balance = walletService.processRollback(traceId, dto, gameSession, vendorService, httpRequestLog);

            //Set Response Data
            responseVo.setResponseCode(ResponseCode.CANCEL_BET_SUCCESS);
            responseVo.setUsername(gameSession.getVendorPlayerUsername());
            responseVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setBalance(balance.setScale(2, RoundingMode.DOWN).doubleValue());

        } catch (BetNotFoundException e) {
            responseVo.setResponseCode(ResponseCode.CANCEL_BET_ROUND_NOT_FOUND);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidRequestException |
                 InvalidPlayerException |
                 JsonProcessingException |
                 GameNotSupportedException |
                 CurrencyNotSupportedException e) {
            responseVo.setResponseCode(ResponseCode.CANCEL_BET_INVALID_PARAMETER);
            httpService.logError(httpRequestLog, e);
        } catch (BetResultIdempotentViolationException e) {
            if (e.getStatus().equals(BetStatus.SETTLED.code)) {
                //if found the bet in settled status
                responseVo.setResponseCode(ResponseCode.CANCEL_BET_ALREADY_ACCEPTED_AND_CANNOT_BE_CANCELED);
            } else if (e.getStatus().equals(BetStatus.REFUNDED.code)) {
                //if found the bet in refunded status
                responseVo.setResponseCode(ResponseCode.CANCEL_BET_ALREADY_CANCELED);
            } else {
                //if found the bet other in settled status (cancel)
                responseVo.setResponseCode(ResponseCode.OTHER_ERROR);
            }
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.CANCEL_BET_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(CancelBetDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CancelBetDto dto, GameSession gameSession)
            throws CurrencyNotSupportedException, GameNotSupportedException {

        //Verify received currency is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(dto.getGame()), GameNotSupportedException::new);
    }
}
