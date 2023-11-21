package com.nextgen.gameaggregator.vendor.pinnacle.api.settled;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsDto;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.ResultVo;

@Service
public class SettledService {
    @Autowired
    private HttpService httpService;
    
    public ResponseVo settled(ActionsDto dto, HttpRequestLog httpRequestLog) {
        ResponseVo responseVo = new ResponseVo();
        ResultVo result = new ResultVo();
        Integer errorCode = ResponseCode.UNKNOWN_ERROR.code;

        try {
            result.setUserCode(dto.getActions().get(0).getPlayerInfo().getUserCode());
            result.setAvailableBalance(BigDecimal.valueOf(10000));
            result.setActions(new ArrayList<>());

            if (!dto.getActions().isEmpty()) {
                CommonVo commonVo = new CommonVo();
                commonVo.setId(dto.getActions().get(0).getId());
                commonVo.setTransactionId(dto.getActions().get(0).getTransaction().getTransactionId());
                commonVo.setWagerId(dto.getActions().get(0).getWagerInfo().getWagerId());
                commonVo.setResponseCode(ResponseCode.SUCCESS.code);

                result.getActions().add(commonVo);
            }

            responseVo.setResult(result);
            responseVo.setErrorCode(ResponseCode.SUCCESS.code);

        } catch (Exception exception) {
            httpService.logError(httpRequestLog, exception);
            responseVo.setErrorCode(errorCode);

        }
        
        return responseVo;
    }
}
