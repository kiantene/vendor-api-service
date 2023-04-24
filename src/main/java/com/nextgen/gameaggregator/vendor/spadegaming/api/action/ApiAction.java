package com.nextgen.gameaggregator.vendor.spadegaming.api.action;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.spadegaming.api.authenticate.AuthenticateService;
import com.nextgen.gameaggregator.vendor.spadegaming.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.spadegaming.api.transfer.TransferService;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.Headers;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.spadegaming.vo.ResponseVo;

@RestController
@RequestMapping(EndPoints.PATH)
public class ApiAction {

    @Autowired
    private HttpService httpService;
    
    @Autowired
    private AuthenticateService authenticateService;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private TransferService transferService;

    // Handle incoming API requests
    @PostMapping
    public ResponseVo handleApiCall(@RequestHeader(Headers.HEADER_KEY_API) String apiAction, HttpServletRequest request) {

        // Switch statement to determine which action to take based on the API request header
        switch (apiAction) {
            case Headers.HEADER_VALUE_AUTHENTICATE:
                return authenticateService.authenticate(request);

            case Headers.HEADER_VALUE_BALANCE:
                return balanceService.balance(request);

            case Headers.HEADER_VALUE_TRANSFER:
                return transferService.transfer(request);

            // If the header does not match any of the expected values, return an error response
            default:
                ResponseVo responseVo = new ResponseVo();
                // Start the HTTP request logging
                HttpRequestLog httpRequestLog = httpService.start(request);
                
                // Get the trace ID from the logging
                String traceId = httpRequestLog.getTraceId();
                responseVo.setResponseCode(ResponseCode.INVALID_PARAMETER);
                responseVo.setSerialNo(traceId);

                // End the HTTP request logging and return the ResponseVo object
                httpService.end(httpRequestLog, responseVo);
                return responseVo;
        }
    }
}
