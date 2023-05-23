package com.nextgen.gameaggregator.vendor.bng.api.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.bng.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.bng.api.bet.TransactionService;
import com.nextgen.gameaggregator.vendor.bng.api.login.LoginService;
import com.nextgen.gameaggregator.vendor.bng.api.logout.LogoutService;
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

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private LogoutService logoutService;

    @PostMapping(path = EndPoints.ACTION)
    public CommonVo action(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Retrieve request body in original string format
        String body = httpRequestLog.getRequestBody();

        // Construct VO
        CommonVo vo = new CommonVo();

        try {

            // Construct this vo for action handling purpose
            ActionDto actionDto = HttpService.convertJsonToDto(body, ActionDto.class);

            // Handle the action and return the resulting value
            vo = this.actionHandling(actionDto, traceId, httpRequestLog);
        } catch (Exception exception) {

        } finally {
            httpService.end(httpRequestLog, vo);
        }


        return vo;
    }

    private CommonVo actionHandling(ActionDto actionDto, String traceId, HttpRequestLog httpRequestLog) throws JsonProcessingException {
        CommonVo vo = new CommonVo();
        switch (actionDto.getName().toLowerCase()) {
            case Actions.LOGIN:
                vo = loginService.login(httpRequestLog, traceId);
                break;
            case Actions.GETBALANCE:
                vo = balanceService.balance(httpRequestLog, traceId);
                break;
            case Actions.TRANSACTION:
                vo = transactionService.transaction(httpRequestLog, traceId);
                break;
            case Actions.LOGOUT:
                vo = logoutService.logout(httpRequestLog, traceId);
                break;
        }
        return vo;
    }
}
