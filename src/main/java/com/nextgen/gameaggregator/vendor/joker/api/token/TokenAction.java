package com.nextgen.gameaggregator.vendor.joker.api.token;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.joker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.joker.constant.ResponseCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class TokenAction {

    @Autowired
    private HttpService httpService;

    @PostMapping(path = EndPoints.TOKEN)
    public TokenVo balance(HttpServletRequest request) throws InvalidRequestException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();

        // Construct VO
        TokenVo tokenVo = new TokenVo();
        tokenVo.setUsername("TESTPLAYER001");
        tokenVo.setBalance(1000.00);
        tokenVo.setResponseCode(ResponseCodes.SUCCESS);

        httpService.end(httpRequestLog, tokenVo);

        return tokenVo;

    }

}
