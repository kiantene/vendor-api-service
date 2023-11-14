package com.nextgen.gameaggregator.vendor.pinnacle.api.balance;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.ResultVo;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class BalanceAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    
    @PostMapping(path = "{agentcode}/wallet/usercode/{usercode}/balance")
    public ResponseVo getBalance(@PathVariable String agentcode, @PathVariable String usercode, HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo responseVo = new ResponseVo();
        ResultVo result = new ResultVo();
        Integer errorCode = ResponseCode.UNKNOWN_ERROR.code;
        String traceId = httpRequestLog.getId();

        try {
            String body = httpRequestLog.getRequestBody();
            CommonDto dto = new Gson().fromJson(body, CommonDto.class);

            GameSession gameSession = gameSessionService.verifyToken(usercode);
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            result.setUserCode(usercode);
            result.setAvailableBalance(BigDecimal.valueOf(1000L));
            responseVo.setResult(result);
            responseVo.setErrorCode(ResponseCode.SUCCESS.code);

        } catch (Exception exception) {
            httpService.logError(httpRequestLog, exception);
            responseVo.setErrorCode(errorCode);

        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        
        return responseVo;
    }
}
