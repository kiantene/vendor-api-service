package com.nextgen.gameaggregator.vendor.saba.api.bet;

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
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class PlaceBetAction {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private SportWalletService sportWalletService;

    @PostMapping(path = EndPoints.PLACE_BET)
    public PlaceBetVo action(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct Vo
        PlaceBetVo vo = new PlaceBetVo();
        BetEvent betEvent = null;

        try {
            // Convert original request body into dto
            RequestDto<PlaceBetDto> dto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), new TypeReference<>() {
            });

            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getMessage().getUserId());

            betEvent = sportWalletService.placeBet(traceId, gameSession, dto.getMessage(), httpRequestLog.getRequestBody(), httpRequestLog);

            vo.setResponseCode(ResponseCode.SUCCESS);
            vo.setRefId(betEvent.getBetInformation().getExternalTransactionId());
            vo.setLicenseeTxId(traceId);

        } catch (BetResultIdempotentViolationException e) {
            vo.setResponseCode(ResponseCode.DUPLICATE_TRANSACTION);

        } catch (InsufficientBalanceException e) {
            vo.setResponseCode(ResponseCode.INSUFFICIENT_BALANCE);

        } catch (Exception e) {
            vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);
            httpService.logError(httpRequestLog, e);

        } finally {
            httpService.end(httpRequestLog, vo);

        }

        return vo;
    }
}
