package com.nextgen.gameaggregator.vendor.poker365.api.Action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.*;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class GeneralAction {

    private final BalanceService balanceService;
    private final BetService betService;
    private final CancelService cancelService;
    private final SettleService settleService;
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final VendorService vendorService;
    private final HttpService httpService;
    private final WalletService walletService;
    private final VendorPlayerService vendorPlayerService;
    Integer vendorPlayerId;

    @Autowired
    public GeneralAction(BalanceService balanceService, BetService betService, CancelService cancelService, SettleService settleService, HttpService httpService,
                         WalletService walletService,
                         VendorService vendorService,
                         GameSessionService gameSessionService,
                         VendorLineService vendorLineService,
                         AgentPlayerService agentPlayerService, VendorPlayerService vendorPlayerService) {
        this.balanceService = balanceService;
        this.betService = betService;
        this.cancelService = cancelService;
        this.settleService = settleService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorPlayerService = vendorPlayerService;
    }

    @PostMapping
    public CommonVo action(HttpServletRequest request) throws JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        CommonVo commonVo = new CommonVo();
        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            CommonDto commonDto = VendorService.convertQueryStringToDtoUrlDecode(body, CommonDto.class);
            String formatedMessageDto = commonDto.getMessage();
            MessageDto messageDto = HttpService.convertJsonToDto(formatedMessageDto, MessageDto.class);

            commonVo = this.actionHandling(messageDto, traceId, httpRequestLog);

//        } catch (InvalidPlayerException e) {
//            balanceVo.setError(ErrorVo.from(ResponseCodes.ERR_PLAYER_NOT_FOUND));
//            httpService.logError(httpRequestLog, e);
//        } catch (AuthenticationException e) {
//            balanceVo.setError(ErrorVo.from(ResponseCodes.ERR_AUTHENTICATION_FAILED));
//            httpService.logError(httpRequestLog, e);
//        } catch (InvalidRequestException e) {
//            balanceVo.setError(ErrorVo.from(ResponseCodes.ERR_REGULATORY_GENERAL));
//            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            commonVo.setStatus(ResponseCodes.FAIL.status);
            commonVo.setMsg(ResponseCodes.FAIL.message);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, commonVo);
        }

        return commonVo;
    }


    private CommonVo actionHandling(MessageDto messageDto, String traceId, HttpRequestLog httpRequestLog) throws
            InvalidRequestException, JsonProcessingException {


        return switch (messageDto.getAction()) {
            case "getBalance" -> balanceService.balance(httpRequestLog, traceId);
            case "bet" -> betService.bet(httpRequestLog, traceId);
            case "cancelBet" -> cancelService.cancel(httpRequestLog, traceId);
            case "settle" -> settleService.settle(httpRequestLog, traceId);
            default -> throw new InvalidRequestException();
        };
    }
}
