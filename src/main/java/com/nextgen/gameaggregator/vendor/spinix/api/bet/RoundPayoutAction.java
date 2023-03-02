package com.nextgen.gameaggregator.vendor.spinix.api.bet;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.spinix.constant.EndPoints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class RoundPayoutAction {

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

    @PostMapping(path = EndPoints.ROUND)
    public RoundPayoutVo bet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();

        // Construct VO
        RoundPayoutDataWalletVo roundPayoutDataWalletVo = new RoundPayoutDataWalletVo();
        RoundPayoutDataVo roundPayoutDataVo = new RoundPayoutDataVo();
        RoundPayoutVo roundPayoutVo = new RoundPayoutVo();

        roundPayoutDataWalletVo.setCurrency("CNY");
        roundPayoutDataWalletVo.setBalance(BigDecimal.valueOf(1000));

        roundPayoutDataVo.setWallet(roundPayoutDataWalletVo);

        roundPayoutVo.setStatus(200);
        roundPayoutVo.setData(roundPayoutDataVo);

        httpService.end(httpRequestLog, roundPayoutVo);

        return roundPayoutVo;
    }
}