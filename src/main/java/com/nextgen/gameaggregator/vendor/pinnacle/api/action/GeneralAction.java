package com.nextgen.gameaggregator.vendor.pinnacle.api.action;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.pinnacle.api.accept.AcceptService;
import com.nextgen.gameaggregator.vendor.pinnacle.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.pinnacle.api.settled.SettledService;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsDto;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.ResponseVo;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class GeneralAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private BetService betService;
    @Autowired
    private AcceptService acceptService;
    @Autowired
    private SettledService settledService;
    
    @PostMapping(path = "/{agentcode}/wagering/usercode/{usercode}/request/{requestid}")
    public ResponseVo handleApiCall(@PathVariable String agentcode, @PathVariable String usercode, @PathVariable String requestid, HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo responseVo = new ResponseVo();
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

    private ResponseVo actionsSwitching(String actionName, ActionsDto dto, HttpRequestLog httpRequestLog) {
        ResponseVo responseVo = new ResponseVo();
    
        switch (actionName) {
            case "BETTED" -> responseVo = betService.bet(dto, httpRequestLog);
            case "ACCEPTED" -> responseVo = acceptService.accept(dto, httpRequestLog);
            case "SETTLED" -> responseVo = settledService.settled(dto, httpRequestLog);
            default -> {
                responseVo.setErrorCode(ResponseCode.UNKNOWN_ERROR.code);
            }
        }
    
        return responseVo;
    }
    
}
