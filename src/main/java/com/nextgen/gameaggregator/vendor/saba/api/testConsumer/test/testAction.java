package com.nextgen.gameaggregator.vendor.saba.api.testConsumer.test;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.sport.entity.SportRawSettledBet;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.saba.api.bet.PlaceBetVo;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.saba.constant.ResponseCode;
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
public class testAction {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private SportWalletService sportWalletService;

    @PostMapping(path = "/testConsumer")
    public PlaceBetVo action(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        GeneralVo vo = new GeneralVo();
        PlaceBetVo pbVo = new PlaceBetVo();

        try {
            String body = httpRequestLog.getRequestBody();
            SportRawSettledBet sportRawSettledBet = HttpService.convertJsonToDto(body, SportRawSettledBet.class);
            BetEvent responseVo = sportWalletService.settle(traceId, sportRawSettledBet, httpRequestLog);
            vo.setBalance(responseVo.getLastBalance());
            vo.setResponseCode(ResponseCode.SUCCESS);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);
            e.printStackTrace();

        } finally {
            httpService.end(httpRequestLog, vo);

        }

        return pbVo;
    }
}
