package com.nextgen.gameaggregator.vendor.saba.api.confirmbet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.saba.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.saba.dto.RequestDto;
import com.nextgen.gameaggregator.vendor.saba.vo.GeneralVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class ConfirmBetAction {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private SportWalletService sportWalletService;

    @PostMapping(path = EndPoints.CONFIRM_BET)
    public GeneralVo action(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct Vo
        GeneralVo vo = new GeneralVo();

        try {
            // Convert original request body into dto
            RequestDto<ConfirmBetDto> dto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), new TypeReference<>() {
            });

            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getMessage().getUserId());

            // 4. Process unsettle data
            BetEvent betEvent = sportWalletService.confirmBet(traceId, gameSession, dto.getMessage(), httpRequestLog.getRequestBody(), httpRequestLog);

            vo.setResponseCode(ResponseCode.SUCCESS);

        } catch (InsufficientBalanceException e) {
            vo.setResponseCode(ResponseCode.INSUFFICIENT_BALANCE);
            httpService.logError(httpRequestLog, e);

        } catch (BetResultIdempotentViolationException e) {
            vo.setResponseCode(ResponseCode.DUPLICATE_TRANSACTION);

        } catch (Exception e) {
            vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);
            vo.setMsg(ResponseCode.SYSTEM_ERROR_RETRY.message);
            httpService.logError(httpRequestLog, e);

        } finally {
            httpService.end(httpRequestLog, vo);

        }

        return vo;
    }
}
