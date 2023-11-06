package com.nextgen.gameaggregator.vendor.pinnacle.api.ping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.ResultVo;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class PingAction {
    @Autowired
    private HttpService httpService;
    
    @PostMapping(path = Endpoints.PING)
    public CommonVo ping(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        CommonVo responseVo = new CommonVo();
        ResultVo result = new ResultVo();
        ResponseCode errorCode = ResponseCode.UNKNOWN_ERROR;
        String timestamp = null;

        try {
            String body = httpRequestLog.getRequestBody();
            PingDto dto = new Gson().fromJson(body, PingDto.class);
            timestamp = dto.getTimestamp();

            result.setAvailable(true);
            responseVo.setResult(result);
            responseVo.setErrorCode(ResponseCode.SUCCESS);
            responseVo.setTimestamp(timestamp);

        } catch (Exception exception) {
            httpService.logError(httpRequestLog, exception);
            responseVo.setErrorCode(errorCode);
            responseVo.setTimestamp(timestamp);

        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        
        return responseVo;
    }
}
