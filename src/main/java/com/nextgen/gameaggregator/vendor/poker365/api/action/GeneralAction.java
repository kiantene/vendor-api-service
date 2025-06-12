package com.nextgen.gameaggregator.vendor.poker365.api.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.poker365.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.poker365.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.poker365.api.cancelbet.CancelService;
import com.nextgen.gameaggregator.vendor.poker365.api.settle.SettleService;
import com.nextgen.gameaggregator.vendor.poker365.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.poker365.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.poker365.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.poker365.service.VendorService;
import com.nextgen.gameaggregator.vendor.poker365.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class GeneralAction {

    private final BalanceService balanceService;
    private final BetService betService;
    private final CancelService cancelService;
    private final SettleService settleService;
    private final HttpService httpService;

    public GeneralAction(BalanceService balanceService,
                         BetService betService,
                         CancelService cancelService,
                         SettleService settleService,
                         HttpService httpService)
    {
        this.balanceService = balanceService;
        this.betService = betService;
        this.cancelService = cancelService;
        this.settleService = settleService;
        this.httpService = httpService;
    }

    @PostMapping
    public CommonVo action(HttpServletRequest request) throws JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        CommonVo commonVo = new CommonVo();
        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            CommonDto commonDto = HttpService.convertQueryStringToDtoUrlDecode(body, CommonDto.class);
            String formatedMessageDto = commonDto.getMessage();
            MessageDto messageDto = HttpService.convertJsonToDto(formatedMessageDto, MessageDto.class);

            this.doValidation(commonDto, messageDto);

            commonVo = this.actionHandling(messageDto, traceId, httpRequestLog);

        } catch (InvalidRequestException e) {
            commonVo.setResponseCodesandMessage(ResponseCodes.INVALID_PARAMETERS);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            commonVo.setResponseCodesandMessage(ResponseCodes.FAIL);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, commonVo);
        }

        return commonVo;
    }

    private void doValidation(CommonDto commonDto, MessageDto messageDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(commonDto);
        ValidationUtils.validateRequest(messageDto);
    }

    private CommonVo actionHandling(MessageDto messageDto, String traceId, HttpRequestLog httpRequestLog) throws
            InvalidRequestException, JsonProcessingException {

            return switch (messageDto.getAction()) {
                case "getBalance" -> balanceService.balance(httpRequestLog, traceId);
                case "bet" -> betService.bet(httpRequestLog);
                case "cancelBet", "voidGame" -> cancelService.cancel(httpRequestLog);
                case "settle" -> settleService.settle(httpRequestLog);
                default -> throw new InvalidRequestException();
            };
    }
}
