package com.nextgen.gameaggregator.vendor.bglive.api.Action;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.aglive.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.aglive.api.endround.SettleService;
import com.nextgen.gameaggregator.vendor.aglive.api.refund.RefundService;
import com.nextgen.gameaggregator.vendor.aglive.service.VendorService;
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
    private final BetService betService;
    private final SettleService settleService;
    private final RefundService refundService;
    private final VendorService vendorService;
    private final VendorLineService vendorLineService;

    @Autowired
    public GeneralAction(HttpService httpService, BalanceService balanceAction, BetService betService, VendorService vendorService,
                         SettleService settleService, RefundService refundService, VendorLineService vendorLineService) {
        this.httpService = httpService;
        this.balanceAction = balanceAction;
        this.betService = betService;
        this.vendorService = vendorService;
        this.settleService = settleService;
        this.refundService = refundService;
        this.vendorLineService = vendorLineService;
    }

    @PostMapping
    public CommonVo action(HttpServletRequest request) throws JsonProcessingException, CredentialNotFoundException {
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

//        } catch (Exception e) {
//            vo.setErrorResponse(ResponseCodes.ERROR);
//            httpService.logError(httpRequestLog, e);

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
