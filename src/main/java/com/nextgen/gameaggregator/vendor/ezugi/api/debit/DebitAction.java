package com.nextgen.gameaggregator.vendor.ezugi.api.debit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.ezugi.api.credit.CreditDto;
import com.nextgen.gameaggregator.vendor.ezugi.api.credit.CreditVo;
import com.nextgen.gameaggregator.vendor.ezugi.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ezugi.constant.ResponseCodes;
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
public class DebitAction {
    @Autowired
    private HttpService httpService;

    @PostMapping(path = EndPoints.DEBIT)
    public CommonVo debit(HttpServletRequest request) throws JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        String body = httpRequestLog.getRequestBody();
        DebitDto debitDto = HttpService.convertJsonToDto(body, DebitDto.class);

        // Construct Vo
        DebitVo debitVo = new DebitVo();
        debitVo.setToken(debitDto.getToken());
        debitVo.setOperatorId(debitDto.getOperatorId());
        debitVo.setUid("testgame1");
        debitVo.setRoundId("11111111111112R");
        debitVo.setTransactionId("11111111111112RT");
        debitVo.setBalance(10.00);
        debitVo.setCurrency("BRL");
        debitVo.setErrorCode(ResponseCodes.COMPLETED_SUCCESSFULLY);
        debitVo.setErrorDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(debitVo.getErrorCode()));
        debitVo.setTimestamp(System.currentTimeMillis());
        httpService.end(httpRequestLog, debitVo);
        return debitVo;
    }
}
