package com.nextgen.gameaggregator.vendor.koolbet.api.sessionBetNsettle;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
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

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class SessionBetNSettleAction {

    private final HttpService httpService;

    private final GameSessionService gameSessionService;

    private final WalletService walletService;

    private final VendorLineService vendorLineService;

    private final VendorService vendorService;


    private final ValidationService validationService;

    @Autowired
    public SessionBetNSettleAction(HttpService httpService, GameSessionService gameSessionService, WalletService walletService,
                                   VendorLineService vendorLineService, VendorService vendorService, ValidationService validationService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorLineService = vendorLineService;
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

            //make a ResultType for bet and settle process indicator
            //ResultType resultType = this.getResultType(betNSettleDto);
            ResultType resultType = vendorService.calculateResultType(sessionBetNSettleDto.getBetAmount(), sessionBetNSettleDto.getWinAmount(), sessionBetNSettleDto.getJackpotAmount(), true);
            //Process full bet data
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, sessionBetNSettleDto, resultType, vendorService, httpRequestLog);

            //Set Response Data
            responseVo.setResponseCode(ResponseCode.SUCCESS);
            responseVo.setUsername(traceId);
            responseVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setBalance(balance.doubleValue());

        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;
    }

    private void doVerification(SessionBetNSettleDto sessionBetNSettleDto, GameSession gameSession) throws
            AuthenticationException, InvalidRequestException, CurrencyNotSupportedException, InvalidPlayerException,
            CredentialNotFoundException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException {

        //Verify received currency is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), sessionBetNSettleDto.getCurrency(), CurrencyNotSupportedException::new);

        //Validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());
    }
}
