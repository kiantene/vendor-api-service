package com.nextgen.gameaggregator.vendor.koolbet.api.reward;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkasia.service.VendorService;
import com.nextgen.gameaggregator.vendor.koolbet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.koolbet.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.koolbet.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class RewardActionKIV {

    private final HttpService httpService;

    private final GameSessionService gameSessionService;

    private final WalletService walletService;

    private final VendorService vendorService;

    @Autowired
    public RewardActionKIV(HttpService httpService, GameSessionService gameSessionService, WalletService walletService,
                           VendorService vendorService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
    }

    @PostMapping(path = EndPoints.REWARD)
    public CommonVo reward(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        CommonVo responseVo = new CommonVo();

        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into rewardDto
            RewardDtoKIV rewardDto = HttpService.convertJsonToDto(body, RewardDtoKIV.class);

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(rewardDto);

            //get rawGameSession by token id
            GameSession gameSession = gameSessionService.verifyToken(rewardDto.getToken());

            //Verify remaining parameters (Verify against database values)
            this.doVerification(rewardDto, gameSession);

            //Process full bet data
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, rewardDto, ResultType.BET_WIN,
                    vendorService, httpRequestLog);

            //Set Response Data
            responseVo.setResponseCode(ResponseCode.REWARD_SUCCESS);
            responseVo.setUsername(gameSession.getVendorPlayerUsername());
            responseVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setBalance(balance);

        } catch (AuthenticationException e) {
            responseVo.setResponseCode(ResponseCode.REWARD_TOKEN_EXPIRED);
            httpService.logError(httpRequestLog, e);
        } catch (BetResultIdempotentViolationException e) {
            responseVo.setResponseCode(ResponseCode.REWARD_ALREADY_ACCEPTED);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidRequestException |
                 JsonProcessingException |
                 GameNotSupportedException |
                 CurrencyNotSupportedException e) {
            responseVo.setResponseCode(ResponseCode.REWARD_INVALID_PARAMETER);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.REWARD_OTHER_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(RewardDtoKIV dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(RewardDtoKIV betNSettleDto, GameSession gameSession) throws
            AuthenticationException, CurrencyNotSupportedException, InvalidPlayerException, DisabledVendorLineException,
            DisabledAgentPlayerException, DisabledGameException, GameNotSupportedException {

        //Verify received currency is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), betNSettleDto.getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(betNSettleDto.getGame()), GameNotSupportedException::new);
    }

}
