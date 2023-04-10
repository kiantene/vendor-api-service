package com.nextgen.gameaggregator.vendor.joker.api.settlebet;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.SettledBetEvent;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.BetHistoryService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.joker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.joker.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.joker.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.RoundingMode;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class SettleBetAction {


    @Autowired
    private HttpService httpService;

    @Autowired
    private GameSessionService gameSessionService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private BetHistoryService betHistoryService;

    @PostMapping(path = EndPoints.SETTLE_BET)
    public CommonVo balance(HttpServletRequest request) throws InvalidRequestException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();

        // Construct VO
        CommonVo commonVo = new CommonVo();
        commonVo.setBalance(1000.00);
        commonVo.setResponseCode(ResponseCodes.SUCCESS);

        try{
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into commonDto
            SettleBetDto settleBetDto = HttpService.convertQueryStringToDtoUrlDecode(body, SettleBetDto.class);

            //Validate request parameters from vendor (Non-database related)
            //this.doValidation(balanceDto);

            //get gameSession by player name in lowercase (vendor return in uppercase)
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(settleBetDto.getUsername().toLowerCase());

            // Verify remaining parameters (Verify against database values)
            //this.doVerification(betDto, gameSession, wToken);

            //Process full bet data
            SettledBetEvent settledBetEvent = walletService.processUnsettleResultSettle(traceId, gameSession, settleBetDto, body);

            //return double balance and success code
            commonVo.setResponseCode(ResponseCodes.SUCCESS);
            commonVo.setBalance(settledBetEvent.getLastBalance().setScale(2, RoundingMode.DOWN).doubleValue());

        } catch (Exception exception) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } finally {
            httpService.end(httpRequestLog, commonVo);
        }

        return commonVo;
    }

}
