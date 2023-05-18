package com.nextgen.gameaggregator.vendor.ezugi.api.rollback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.ezugi.api.debit.DebitDto;
import com.nextgen.gameaggregator.vendor.ezugi.api.debit.DebitVo;
import com.nextgen.gameaggregator.vendor.ezugi.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ezugi.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ezugi.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class RollbackAction {

    @Autowired
    private HttpService httpService;

    @PostMapping(path = EndPoints.ROLLBACK)
    public CommonVo rollback(HttpServletRequest request) throws JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        String body = httpRequestLog.getRequestBody();
        RollbackDto rollbackDto = HttpService.convertJsonToDto(body, RollbackDto.class);

        // Construct Vo
        RollbackVo rollbackVo = new RollbackVo();
        rollbackVo.setToken(rollbackDto.getToken());
        rollbackVo.setOperatorId(rollbackDto.getOperatorId());
        rollbackVo.setUid("testgame1");
        rollbackVo.setRoundId("11111111111112R");
        rollbackVo.setTransactionId("11111111111112RT");
        rollbackVo.setBalance(50.00);
        rollbackVo.setCurrency("BRL");
        rollbackVo.setErrorCode(ResponseCodes.COMPLETED_SUCCESSFULLY);
        rollbackVo.setErrorDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(rollbackVo.getErrorCode()));
        rollbackVo.setTimestamp(System.currentTimeMillis());
        httpService.end(httpRequestLog, rollbackVo);
        return rollbackVo;
    }
}
