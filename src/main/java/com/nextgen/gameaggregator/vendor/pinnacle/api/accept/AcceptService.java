package com.nextgen.gameaggregator.vendor.pinnacle.api.accept;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsDto;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.ResultVo;

@Service
public class AcceptService {
    @Autowired
    private HttpService httpService;
    
    public ResponseVo accept(ActionsDto dto, HttpRequestLog httpRequestLog) {
        ResponseVo responseVo = new ResponseVo();
        ResultVo result = new ResultVo();
        Integer errorCode = ResponseCode.UNKNOWN_ERROR.code;

        try {
            AcceptActionsDto acceptActionsDto = new ObjectMapper().convertValue(dto, AcceptActionsDto.class);
            result.setUserCode(acceptActionsDto.getActions().get(0).getPlayerInfo().getUserCode());
            result.setAvailableBalance(BigDecimal.valueOf(10000));
            result.getActions().setId(acceptActionsDto.getActions().get(0).getId());
            result.getActions().setWagerId(acceptActionsDto.getActions().get(0).getWagerInfo().getWagerId());
            result.getActions().setResponseCode(ResponseCode.SUCCESS);

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
