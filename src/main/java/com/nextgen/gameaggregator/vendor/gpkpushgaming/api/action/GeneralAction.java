package com.nextgen.gameaggregator.vendor.gpkpushgaming.api.action;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.api.rollback.RollBackService;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.constant.Actions;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.dto.ActionDto;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class GeneralAction {

    private final HttpService httpService;
    private final BalanceService balanceService;
    private final BetService betService;
    private final RollBackService rollBackService;

    public GeneralAction(HttpService httpService, BalanceService balanceService, BetService betService, RollBackService rollBackService) {
        this.httpService = httpService;
        this.balanceService = balanceService;
        this.betService = betService;
        this.rollBackService = rollBackService;
    }

    @PostMapping(path = EndPoints.ACTION)
    public CommonVo action(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        CommonVo vo = new CommonVo();

        try {
            String body = httpRequestLog.getRequestBody();

            // Construct this vo for action handling purpose
            ActionDto actionDto = HttpService.convertQueryStringToDto(body, ActionDto.class);

            // Validate the actionDto object
            this.doValidation(actionDto);

            vo = this.actionHandling(actionDto, traceId, httpRequestLog);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.ERROR.code);
        } finally {
            httpService.end(httpRequestLog, vo);
        }

        return vo;
    }

    private CommonVo actionHandling(ActionDto actionDto, String traceId, HttpRequestLog httpRequestLog) {
        CommonVo vo = new CommonVo();

        return switch (actionDto.getCmd()) {
            case Actions.BALANCE -> balanceService.balance(httpRequestLog, traceId);
            case Actions.BET_SETTLE -> betService.transaction(httpRequestLog, traceId);
            case Actions.ROLLBACK -> rollBackService.rollback(httpRequestLog, traceId);
            default -> vo;
        };
    }

    private void doValidation(ActionDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }
}
