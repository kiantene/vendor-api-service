package com.nextgen.gameaggregator.vendor.bombay.api.debit;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.bombay.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.bombay.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bombay.service.VendorService;
import com.nextgen.gameaggregator.vendor.bombay.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path= EndPoints.PATH)
@Slf4j
public class DebitAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    VendorService vendorService;
    @Autowired
    private WalletAdjustmentService walletAdjustmentService;
    @Autowired
    private ValidationService validationService;

    @PostMapping(path = EndPoints.DEBIT)
    public ResponseVo debit(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        ResponseVo responseVo = new ResponseVo();

        DebitDto debitDto = null;

        try{
            String body = httpRequestLog.getRequestBody();

            debitDto = HttpService.convertJsonToDto(body, DebitDto.class);

            responseVo.setStatus("testing");

        } catch(Exception e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_UNKNOWN);
        } finally{
            responseVo.setRequest_uuid(debitDto.getRequest_uuid());
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }
}
