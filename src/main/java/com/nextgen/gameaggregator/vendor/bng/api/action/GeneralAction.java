package com.nextgen.gameaggregator.vendor.bng.api.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.bng.api.balance.BalanceService;
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

    @Autowired
    private BalanceService balanceService;

    @PostMapping(path = EndPoints.ACTION)
    public CommonVo action(HttpServletRequest request) throws JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Retrieve request body in original string format
        String body = httpRequestLog.getRequestBody();

        // Construct this vo for action handling purpose
        ActionDto actionDto = HttpService.convertJsonToDto(body, ActionDto.class);

        // Construct VO
        CommonVo vo = new CommonVo();

        // Handle the action and return the resulting value
        vo = this.actionHandling(actionDto, traceId, body);

        httpService.end(httpRequestLog, vo);

        return vo;
    }

    private CommonVo actionHandling(ActionDto actionDto, String traceId, String body) throws JsonProcessingException {
        CommonVo vo = new CommonVo();
        System.out.print("serhrs-"+actionDto.getName().toLowerCase());
        switch (actionDto.getName().toLowerCase()) {
            case Actions.LOGIN:
                vo = loginService.login(body, traceId);
                break;
            case Actions.GETBALANCE:
                vo = balanceService.balance(body, traceId);
                break;
        }
        return vo;
    }
}
