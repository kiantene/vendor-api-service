package com.nextgen.gameaggregator.vendor.pinnacle.api.bet;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsDto;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.ResultVo;

@Service
public class BetService {
    @Autowired
    private HttpService httpService;
    
    public CommonVo bet(ActionsDto dto, HttpRequestLog httpRequestLog) {
        CommonVo responseVo = new CommonVo();
        ResultVo result = new ResultVo();
        Integer errorCode = ResponseCode.UNKNOWN_ERROR.code;

        try {
            result.setUserCode(dto.getActions().get(0).getPlayerInfo().getUserCode());
            result.setAvailableBalance(BigDecimal.valueOf(10000));
            result.getActions().setId(dto.getActions().get(0).getId());
            result.getActions().setTransactionId(dto.getActions().get(0).getTransaction().getTransactionId()); 
            result.getActions().setWagerId(dto.getActions().get(0).getWagerInfo().getWagerId());
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
