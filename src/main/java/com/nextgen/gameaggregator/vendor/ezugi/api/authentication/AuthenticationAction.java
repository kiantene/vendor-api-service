package com.nextgen.gameaggregator.vendor.ezugi.api.authentication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.ezugi.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ezugi.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ezugi.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class AuthenticationAction {
    @Autowired
    private HttpService httpService;

    @PostMapping(path = EndPoints.AUTHENTICATION)
    public CommonVo authenticate(HttpServletRequest request) throws JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        String body = httpRequestLog.getRequestBody();
        AuthenticationDto authenticationDto = HttpService.convertJsonToDto(body, AuthenticationDto.class);

        // Construct Vo
        AuthenticationVo authenticationVo = new AuthenticationVo();
        authenticationVo.setToken(authenticationDto.getToken()+"ST");
        authenticationVo.setOperatorId(authenticationDto.getOperatorId());
        authenticationVo.setUid("testgame1");
        authenticationVo.setBalance(100.00);
        authenticationVo.setCurrency("BRL");
        authenticationVo.setErrorCode(ResponseCodes.COMPLETED_SUCCESSFULLY);
        authenticationVo.setErrorDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(authenticationVo.getErrorCode()));
        authenticationVo.setTimestamp(System.currentTimeMillis());
        httpService.end(httpRequestLog, authenticationVo);
        return authenticationVo;
    }
}
