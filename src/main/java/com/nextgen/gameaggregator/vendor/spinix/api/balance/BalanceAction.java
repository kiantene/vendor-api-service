package com.nextgen.gameaggregator.vendor.spinix.api.balance;

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
public class BalanceAction {

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

    @PostMapping(path = EndPoints.BALANCE)
    public BalanceVo balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();

        // Construct VO
        BalanceVo balanceVo = new BalanceVo();
        BalanceDataVo balanceDataVo = new BalanceDataVo();
        BalanceDataWalletVo balanceDataWalletVo = new BalanceDataWalletVo();

        balanceDataWalletVo.setCurrency("CNY");
        balanceDataWalletVo.setBalance(BigDecimal.valueOf(1000));

        balanceDataVo.setWallet(balanceDataWalletVo);

        balanceVo.setStatus(200);
        balanceVo.setData(balanceDataVo);

        httpService.end(httpRequestLog, balanceVo);

        return balanceVo;

    }
}
