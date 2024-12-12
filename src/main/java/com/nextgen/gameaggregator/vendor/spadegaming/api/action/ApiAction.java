package com.nextgen.gameaggregator.vendor.spadegaming.api.action;

import org.springframework.web.bind.annotation.*;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.spadegaming.api.authenticate.AuthenticateService;
import com.nextgen.gameaggregator.vendor.spadegaming.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.spadegaming.api.transfer.TransferService;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.Headers;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.spadegaming.vo.ResponseVo;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(EndPoints.PATH)
public class ApiAction {

    private final HttpService httpService;
    private final AuthenticateService authenticateService;
    private final BalanceService balanceService;
    private final TransferService transferService;

    public ApiAction(HttpService httpService, 
                     AuthenticateService authenticateService, 
                     BalanceService balanceService, 
                     TransferService transferService) {
        this.httpService = httpService;
        this.authenticateService = authenticateService;
        this.balanceService = balanceService;
        this.transferService = transferService;
    }

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
                String traceId = httpRequestLog.getId();
                responseVo.setResponseCode(ResponseCode.INVALID_PARAMETER);
                responseVo.setSerialNo(traceId);

                // End the HTTP request logging and return the ResponseVo object
                httpService.end(httpRequestLog, responseVo);
                return responseVo;
        }
    }
}
