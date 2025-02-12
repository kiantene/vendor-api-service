package com.nextgen.gameaggregator.vendor.bglive.api.Action;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.bglive.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.bglive.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.bglive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bglive.vo.CommonVo;
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

    private final HttpService httpService;
    private final BalanceService balanceAction;


    @Autowired
    public GeneralAction(HttpService httpService, BalanceService balanceAction) {
        this.httpService = httpService;
        this.balanceAction = balanceAction;
    }

    @PostMapping
    public CommonVo action(HttpServletRequest request) throws JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        CommonVo commonVo = new CommonVo();

        try {
            String body = httpRequestLog.getRequestBody();
            ActionDto actionDto = HttpService.convertJsonToDto(body, ActionDto.class);
            // Handle the action and return the resulting value
            commonVo = this.actionHandling(actionDto, traceId, httpRequestLog);

        } catch (JsonProcessingException | InvalidRequestException e) {
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.SYSTEM_ERROR.code, ResponseCodes.SYSTEM_ERROR.message, ResponseCodes.SYSTEM_ERROR.message);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.MISSING_PARAMETERS.code, ResponseCodes.MISSING_PARAMETERS.message, ResponseCodes.MISSING_PARAMETERS.message);
            httpService.logError(httpRequestLog, e);

        } finally {
            httpService.end(httpRequestLog, commonVo);
        }
        return commonVo;
    }

    private CommonVo actionHandling(ActionDto actionDto, String traceId, HttpRequestLog httpRequestLog) throws InvalidRequestException {

        return switch (actionDto.getMethod()) {
            case "open.operator.user.balance" -> balanceAction.balance(httpRequestLog, traceId);
//            case "open.operator.order.transfer" -> settleService.settle(httpRequestLog, traceId);
//            case "open.operator.calc.transfer" -> refundService.refund(httpRequestLog, traceId);
            default -> throw new InvalidRequestException();

        };
    }
}
