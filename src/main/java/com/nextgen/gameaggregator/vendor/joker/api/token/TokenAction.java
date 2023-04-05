package com.nextgen.gameaggregator.vendor.joker.api.token;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.joker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.joker.constant.ResponseCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class TokenAction {

    @Autowired
    private HttpService httpService;

    @Autowired
    private GameSessionService gameSessionService;

    @Autowired
    private WalletService walletService;

    @PostMapping(path = EndPoints.TOKEN)
    public TokenVo balance(HttpServletRequest request) throws InvalidRequestException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();

        // Construct VO
        TokenVo tokenVo = new TokenVo();
//        tokenVo.setUsername("TESTPLAYER001");
//        tokenVo.setBalance(1000.00);
//        tokenVo.setResponseCode(ResponseCodes.SUCCESS);
        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into commonDto
            TokenDto tokenDto = HttpService.convertQueryStringToDtoUrlDecode(body, TokenDto.class);

            //Validate request parameters from vendor (Non-database related)
            //this.doValidation(tokenDto);

            //get gameSession by player name and vendor game id
            GameSession gameSession = gameSessionService.verifyToken(tokenDto.getToken());

            //Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            //Verify remaining parameters (Verify against database values)
            //this.doVerification(commonDto, balanceDto, gameSession, jsonParam);

            //return double balance and success code
            tokenVo.setResponseCode(ResponseCodes.SUCCESS);
            tokenVo.setBalance(balance.setScale(2, RoundingMode.DOWN).doubleValue());
            tokenVo.setUsername(gameSession.getVendorPlayerUsername());


        } catch (Exception exception) {
            tokenVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } finally {
            httpService.end(httpRequestLog, tokenVo);
        }

        return tokenVo;

    }

}
