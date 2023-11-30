package com.nextgen.gameaggregator.vendor.yeebet.api.balance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.yeebet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.yeebet.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(path= EndPoints.PATH)
@Slf4j
public class BalanceAction {
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

    @GetMapping (path = EndPoints.BALANCE)
    public ResponseVo balance(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        // Get all headers
        Enumeration<String> headerNames = request.getHeaderNames();
        Enumeration<String> paramNames = request.getParameterNames();

        ResponseVo vo = new ResponseVo();
        vo.setHeadersMap(new HashMap<>());
        vo.setParamsMap(new HashMap<>());

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            vo.getHeadersMap().put(headerName, headerValue);
        }
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
            vo.getParamsMap().put(paramName, paramValue);
        }

        httpService.end(httpRequestLog, vo);

        return vo;
    }

    private String mapToJson(Map<String, String> params){
        ObjectMapper objectMapper = new ObjectMapper();

        String jsonString = null;

        try{
            jsonString = objectMapper.writeValueAsString(params);

        }catch(Exception e){
            jsonString = "{}";
        }

        return jsonString;
    }

    @Data
    static class ResponseVo implements HttpResponse {

        private Map<String, String> headersMap;
        private Map<String, String> paramsMap;


        @Override
        public boolean hasError() {
            return false;
        }
    }

}