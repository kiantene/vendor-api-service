package com.nextgen.gameaggregator.vendor.ezugi.api.credit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.ezugi.api.authentication.AuthenticationDto;
import com.nextgen.gameaggregator.vendor.ezugi.api.authentication.AuthenticationVo;
import com.nextgen.gameaggregator.vendor.ezugi.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ezugi.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ezugi.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.ezugi.vo.CommonVo;
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
public class CreditAction extends CommonDto {
    @Autowired
    private HttpService httpService;

    @PostMapping(path = EndPoints.CREDIT)
    public CommonVo credit(HttpServletRequest request) throws JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        String body = httpRequestLog.getRequestBody();
        CreditDto creditDto = HttpService.convertJsonToDto(body, CreditDto.class);

        // Construct Vo
        CreditVo creditVo = new CreditVo();
        creditVo.setToken(creditDto.getToken());
        creditVo.setOperatorId(creditDto.getOperatorId());
        creditVo.setUid("testgame1");
        creditVo.setRoundId("11111111111111R");
        creditVo.setTransactionId("11111111111111RT");
        creditVo.setBalance(BigDecimal.valueOf(200.00));
        creditVo.setCurrency("BRL");
        creditVo.setErrorCode(ResponseCodes.COMPLETED_SUCCESSFULLY);
        creditVo.setErrorDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(creditVo.getErrorCode()));
        creditVo.setTimestamp(System.currentTimeMillis());
        httpService.end(httpRequestLog, creditVo);
        return creditVo;
    }
}
