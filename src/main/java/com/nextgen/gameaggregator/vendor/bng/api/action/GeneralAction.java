package com.nextgen.gameaggregator.vendor.bng.api.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.bng.api.login.LoginService;
import com.nextgen.gameaggregator.vendor.bng.constant.EndPoints;

import com.nextgen.gameaggregator.vendor.bng.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.bng.constant.Actions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class GeneralAction {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;

    @Autowired
    private VendorLineService vendorLineService;

    @Autowired
    private LoginService loginService;

    @PostMapping(path = EndPoints.ACTION)
    public CommonVo action(HttpServletRequest request) throws JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        String body = httpRequestLog.getRequestBody();

        ActionDto actionDto = HttpService.convertJsonToDto(body, ActionDto.class);

        // Construct VO
        CommonVo vo = new CommonVo();

        LoginResponseDto responseDto = new LoginResponseDto();

        // Handle the action and return the resulting value
        vo = this.actionHandling(actionDto, traceId, body);

        httpService.end(httpRequestLog, responseDto);

        return vo;
    }

    private CommonVo actionHandling(ActionDto actionDto, String traceId, String body) throws JsonProcessingException {
        CommonVo vo = new CommonVo();
        switch (actionDto.getName()) {
            case Actions.LOGIN:
                vo = loginService.login(body, traceId);
        }
        return vo;
    }
}
