package com.nextgen.gameaggregator.vendor.poker365.api.Action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.poker365.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.poker365.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.poker365.constant.ResponseCodes;
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
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final VendorService vendorService;
    private final HttpService httpService;
    private final WalletService walletService;
    private final VendorPlayerService vendorPlayerService;
    Integer vendorPlayerId;

    @Autowired
    public GeneralAction(BalanceService balanceService, HttpService httpService,
                         WalletService walletService,
                         VendorService vendorService,
                         GameSessionService gameSessionService,
                         VendorLineService vendorLineService,
                         AgentPlayerService agentPlayerService, VendorPlayerService vendorPlayerService) {
        this.balanceService = balanceService;
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
            ActionDto actionDto = HttpService.convertJsonToDto(body, ActionDto.class);
            commonVo = this.actionHandling(actionDto, traceId, httpRequestLog);

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


    private CommonVo actionHandling(ActionDto actionDto, String traceId, HttpRequestLog httpRequestLog) throws
            InvalidRequestException {


        return switch (actionDto.getMessage().getAction()) {
            case "getBalance" -> balanceService.balance(httpRequestLog, traceId);
//            case "WITHDRAW" -> betService.bet(httpRequestLog, traceId);
//            case "DEPOSIT" -> settleService.settle(httpRequestLog, traceId);
//            case "ROLLBACK" -> rollBackService.rollback(httpRequestLog, traceId);
            default -> throw new InvalidRequestException();
        };
    }
}
