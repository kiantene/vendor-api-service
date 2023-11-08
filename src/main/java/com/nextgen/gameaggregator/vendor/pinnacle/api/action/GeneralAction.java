package com.nextgen.gameaggregator.vendor.pinnacle.api.action;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.pinnacle.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsDto;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.CommonVo;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class GeneralAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private BetService betService;
    
    @PostMapping(path = "/{agentcode}/wagering/usercode/{usercode}/request/{requestid}")
    public CommonVo handleApiCall(@PathVariable String agentcode, @PathVariable String usercode, @PathVariable String requestid, HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        CommonVo responseVo = new CommonVo();
        Integer errorCode = ResponseCode.UNKNOWN_ERROR.code;

        try {
            String body = httpRequestLog.getRequestBody();
            ObjectMapper objectMapper = new ObjectMapper();
            ActionsDto dto = objectMapper.readValue(body, ActionsDto.class);

            // Get action name from the first element in list
            String actionName = dto.getActions().get(0).getName();
            responseVo = actionsSwitching(actionName, dto, httpRequestLog);
            
        } catch (Exception exception) {
            httpService.logError(httpRequestLog, exception);
            responseVo.setErrorCode(errorCode);

        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        
        return responseVo;
    }

    private CommonVo actionsSwitching(String actionName, ActionsDto dto, HttpRequestLog httpRequestLog) {
        CommonVo responseVo = new CommonVo();
    
        if ("BETTED".equals(actionName)) {
            responseVo = betService.bet(dto, httpRequestLog);
        }

        return responseVo;
    }
}
