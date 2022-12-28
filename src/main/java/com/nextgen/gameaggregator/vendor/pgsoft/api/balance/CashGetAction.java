package com.nextgen.gameaggregator.vendor.pgsoft.api.balance;


import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.pgsoft.vo.ResponseVo;
import com.nextgen.sas.core.web.wrapper.WebRequestWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestScope
@RequestMapping(path = Endpoints.PATH)
public class CashGetAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;

    @PostMapping(path = Endpoints.BALANCE)
    public ResponseVo<CashGetVo> authenticate(WebRequestWrapper request) {
        ResponseVo<CashGetVo> parentResponseVo = new ResponseVo<>();
        CashGetVo responseVo = new CashGetVo();
        parentResponseVo.setData(responseVo);
        HttpRequestLog httpRequestLog = httpService.logRequest(request);
        String traceId = UUID.randomUUID().toString();

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();
        } catch (Exception exception) { // any other exception encountered
            parentResponseVo.setError(ResponseCodes.INTERNAL_SERVER_ERROR);
            httpRequestLog.setErrorMessage(HttpService.getStackTrace(exception));

        } finally {
            if (!parentResponseVo.getError().equals(null)) {
                httpRequestLog.setStatus(HttpService.ERROR);
            }
            httpRequestLog.setEndTime(System.currentTimeMillis());
            ConcurrencyService.THREAD_POOL.submit(() -> httpService.logResponse(httpRequestLog, responseVo, traceId));
        }


        //*
        responseVo.setUpdatedTime(1020202020L);
        responseVo.setBalanceAmount(BigDecimal.valueOf(22.3));
        responseVo.setCurrencyCode("CNY");
        return parentResponseVo;
    }

}