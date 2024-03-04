package com.nextgen.gameaggregator.vendor.saba.api.test;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.saba.api.bet.PlaceBetVo;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.saba.constant.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class testAction {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private SportWalletService sportWalletService;

    @PostMapping(path = "/test")
    public PlaceBetVo action(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct Vo
        PlaceBetVo vo = new PlaceBetVo();
        BetEvent betEvent = null;

        try {

            String body = httpRequestLog.getRequestBody();
            testDto dto = HttpService.convertJsonToDto(body, testDto.class);

            System.out.println("TEST DTO");
            System.out.println(dto);

            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getVendorPlayerUsername());
            sportWalletService.settle(traceId, dto, httpRequestLog);

            vo.setResponseCode(ResponseCode.SUCCESS);
            vo.setRefId("SUCCESS");
            vo.setLicenseeTxId(traceId);

//
//        } catch (BetResultIdempotentViolationException e) {
//            vo.setResponseCode(ResponseCode.DUPLICATE_TRANSACTION);
//
//        } catch (InsufficientBalanceException e) {
//            vo.setResponseCode(ResponseCode.INSUFFICIENT_BALANCE);

        } catch (Exception e) {
            vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);
            httpService.logError(httpRequestLog, e);

        } finally {
            httpService.end(httpRequestLog, vo);

        }

        return vo;
    }
}
