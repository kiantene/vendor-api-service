package com.nextgen.gameaggregator.vendor.bglive.api.action;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.bglive.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.bglive.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.bglive.api.ping.PingService;
import com.nextgen.gameaggregator.vendor.bglive.api.query.QueryService;
import com.nextgen.gameaggregator.vendor.bglive.api.settlement.SettlementService;
import com.nextgen.gameaggregator.vendor.bglive.api.transfer.TransferService;
import com.nextgen.gameaggregator.vendor.bglive.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.bglive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bglive.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.bglive.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class GeneralAction {

    private final HttpService httpService;
    private final BalanceService balanceService;
    private final BetService betService;
    private final SettlementService settlementService;
    private final QueryService queryService;
    private final TransferService transferService;
    private final PingService pingService;


    @Autowired
    public GeneralAction(HttpService httpService, BalanceService balanceService, BetService betService,
                         SettlementService settlementService, QueryService queryService, TransferService transferService, PingService pingService) {
        this.httpService = httpService;
        this.balanceService = balanceService;
        this.betService = betService;
        this.settlementService = settlementService;
        this.queryService = queryService;
        this.transferService = transferService;
        this.pingService = pingService;
    }

    @PostMapping
    public CommonVo action(HttpServletRequest request) throws JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        CommonVo commonVo = new CommonVo();

        try {
            String body = httpRequestLog.getRequestBody();
            CommonDto commonDto = HttpService.convertJsonToDto(body, CommonDto.class);
            // Handle the action and return the resulting value
            commonVo = this.actionHandling(commonDto, traceId, httpRequestLog, request);

        } catch (JsonProcessingException | InvalidRequestException e) {
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.SYSTEM_ERROR.code,
                    ResponseCodes.SYSTEM_ERROR.message);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.MISSING_PARAMETERS.code,
                    ResponseCodes.MISSING_PARAMETERS.message);
            httpService.logError(httpRequestLog, e);

        } finally {
            httpService.end(httpRequestLog, commonVo);
        }
        return commonVo;
    }

    private CommonVo actionHandling(CommonDto commonDto, String traceId, HttpRequestLog httpRequestLog, HttpServletRequest httpServletRequest)
            throws InvalidRequestException {
        return switch (commonDto.getMethod()) {
            case "open.operator.user.balance" -> balanceService.balance(httpRequestLog, traceId);
            case "open.operator.order.transfer" -> betService.bet(httpRequestLog, httpServletRequest);
            case "open.operator.calc.transfer" -> settlementService.settle(httpRequestLog, httpServletRequest);
            case "open.operator.order.status" -> queryService.query(httpRequestLog, httpServletRequest);
            case "open.operator.user.transfer" -> transferService.transfer(httpRequestLog);
            case "open.operator.ping" -> pingService.ping(httpRequestLog);
            default -> throw new InvalidRequestException();
        };
    }
}
