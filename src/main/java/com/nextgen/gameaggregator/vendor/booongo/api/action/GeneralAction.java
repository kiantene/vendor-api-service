package com.nextgen.gameaggregator.vendor.booongo.api.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.booongo.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.booongo.api.bet.TransactionService;
import com.nextgen.gameaggregator.vendor.booongo.api.freespin.FreeSpinService;
import com.nextgen.gameaggregator.vendor.booongo.api.login.LoginService;
import com.nextgen.gameaggregator.vendor.booongo.api.refund.RollbackService;
import com.nextgen.gameaggregator.vendor.booongo.constant.Actions;
import com.nextgen.gameaggregator.vendor.booongo.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.booongo.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.booongo.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.booongo.vo.ErrorVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
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
    private LoginService loginService;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private RollbackService rollbackService;

    @Autowired
    private FreeSpinService freeSpinService;


    @PostMapping(path = EndPoints.ACTION)
    public ResponseEntity<CommonVo> action(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Retrieve request body in original string format
        String body = httpRequestLog.getRequestBody();

        // Construct VO
        CommonVo vo = new CommonVo();
        ErrorVo error = new ErrorVo();
        Integer httpStatus = HttpStatus.SC_OK; //default is 200 status

        try {

            // Construct this vo for action handling purpose
            ActionDto actionDto = HttpService.convertJsonToDto(body, ActionDto.class);

            // Validate the actionDto object
            this.doValidation(actionDto);

            // Handle the action and return the resulting value
            vo = this.actionHandling(actionDto, traceId, httpRequestLog);

        } catch (InvalidRequestException |
                 JsonProcessingException e) {
            error.setCode(ResponseCodes.OTHER_EXCEED);
            vo.setError(error);
        } finally {
            httpService.end(httpRequestLog, vo);
        }

        //check the processed data included httpstatus or not
        if(vo.getError() != null && vo.getError().getHttpStatus() != null){
            httpStatus = vo.getError().getHttpStatus();
            vo.getError().setHttpStatus(null);
        }

        return new ResponseEntity<>(vo, HttpStatusCode.valueOf(httpStatus));
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

                // check the condition to decide which bet process should be taken
                if(actionDto.getArgs().getBonus() != null){
                    vo = freeSpinService.freespin(httpRequestLog, traceId);
                }else{
                    vo = transactionService.transaction(httpRequestLog, traceId);
                }

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
