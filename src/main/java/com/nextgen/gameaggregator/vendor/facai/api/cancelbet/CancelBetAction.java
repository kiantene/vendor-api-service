package com.nextgen.gameaggregator.vendor.facai.api.cancelbet;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.facai.constant.EndPoints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CancelBetAction {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private WalletService walletService;

    @PostMapping(path = EndPoints.CANCEL_SLOT_BET)
    public CancelBetVo cancelbet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();

        // Construct VO
        CancelBetVo cancelBetVo = new CancelBetVo();
        cancelBetVo.setResult(0);
        cancelBetVo.setMainPoints(1000.00);

        httpService.end(httpRequestLog, cancelBetVo);

        return cancelBetVo;

    }
}
