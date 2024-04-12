package com.nextgen.gameaggregator.vendor.gpkasia.api.action;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkasia.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.gpkasia.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.gpkasia.api.rollback.RollBackService;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.Actions;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.gpkasia.dto.ActionDto;
import com.nextgen.gameaggregator.vendor.gpkasia.vo.CommonVo;
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
    private BalanceService balanceService;
    @Autowired
    private BetService betService;
    @Autowired
    private RollBackService rollBackService;

    @PostMapping(path = EndPoints.ACTION)
    public CommonVo action(HttpServletRequest request){
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        CommonVo vo = new CommonVo();

        try{
            String body = httpRequestLog.getRequestBody();

            // Construct this vo for action handling purpose
            ActionDto actionDto = httpService.convertQueryStringToDto(body, ActionDto.class);

            // Validate the actionDto object
            this.doValidation(actionDto);

            vo = this.actionHandling(actionDto, traceId, httpRequestLog);

        } catch (InvalidRequestException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.ERROR);
        } finally{
            httpService.end(httpRequestLog, vo);
        }

        return vo;
    }

    private CommonVo actionHandling(ActionDto actionDto, String traceId, HttpRequestLog httpRequestLog){
        CommonVo vo = new CommonVo();

        switch (actionDto.getCmd()) {
            case Actions.BALANCE:
                vo = balanceService.balance(httpRequestLog, traceId);
                break;
            case Actions.BET_SETTLE:
                vo = betService.transaction(httpRequestLog, traceId);
                break;
            case Actions.ROLLBACK:
                vo = rollBackService.rollback(httpRequestLog, traceId);
                break;
        }

        return vo;
    }

    private void doValidation(ActionDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }
}
