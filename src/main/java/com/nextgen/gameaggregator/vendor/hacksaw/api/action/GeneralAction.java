package com.nextgen.gameaggregator.vendor.hacksaw.api.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.hacksaw.api.authenticate.AuthService;
import com.nextgen.gameaggregator.vendor.hacksaw.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.hacksaw.api.bet.TransactionService;
import com.nextgen.gameaggregator.vendor.hacksaw.api.endround.CreditService;
import com.nextgen.gameaggregator.vendor.hacksaw.api.rollback.RollbackService;
import com.nextgen.gameaggregator.vendor.hacksaw.constant.Actions;
import com.nextgen.gameaggregator.vendor.hacksaw.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.hacksaw.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.hacksaw.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class GeneralAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private AuthService authenticationService;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private CreditService creditService;
    @Autowired
    private RollbackService rollbackService;

    @PostMapping(path = EndPoints.ACTION)
    public ResponseVo action(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Retrieve request body in original string format
        String body = httpRequestLog.getRequestBody();

        // Construct VO
        ResponseVo vo = new ResponseVo();

        try {

            // Construct this vo for action handling purpose
            ActionDto actionDto = HttpService.convertJsonToDto(body, ActionDto.class);

            // Validate the actionDto object
            this.doValidation(actionDto);

            // Handle the action and return the resulting value
            vo = this.actionHandling(actionDto, traceId, httpRequestLog);

        } catch (InvalidRequestException |
                 JsonProcessingException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_ACTION);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCodes(ResponseCodes.GENERAL_ERROR);

        } finally {
            httpService.end(httpRequestLog, vo);
        }

        return vo;
    }

    private ResponseVo actionHandling(ActionDto actionDto, String traceId, HttpRequestLog httpRequestLog) {
        ResponseVo vo = new ResponseVo();

        switch (actionDto.getAction()) {
            case Actions.AUTHENTICATE:
                vo = authenticationService.authenticate(httpRequestLog, traceId);
                break;
            case Actions.BALANCE:
                vo = balanceService.balance(httpRequestLog, traceId);
                break;
            case Actions.BET:
                vo = transactionService.transaction(httpRequestLog, traceId);
                break;
            case Actions.CREDIT:
                vo = creditService.credit(httpRequestLog, traceId);
                break;
            case Actions.ROLLBACK:
                vo = rollbackService.rollback(httpRequestLog, traceId);
                break;
        }
        return vo;
    }

    private void doValidation(ActionDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }
}
