package com.nextgen.gameaggregator.vendor.pinnacle.api.ping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.ResultVo;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class PingAction {
    @Autowired
    private HttpService httpService;
    
    @PostMapping(path = Endpoints.PING)
    public ResponseVo ping(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo responseVo = new ResponseVo();
        ResultVo result = new ResultVo();
        Integer errorCode = ResponseCode.UNKNOWN_ERROR.code;

        try {
            result.setAvailable(true);
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
