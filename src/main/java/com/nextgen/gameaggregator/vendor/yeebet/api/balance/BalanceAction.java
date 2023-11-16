package com.nextgen.gameaggregator.vendor.yeebet.api.balance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.yeebet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.yeebet.service.VendorService;
import com.nextgen.gameaggregator.vendor.yeebet.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseVo balance(HttpServletRequest request, @RequestParam String appid, @RequestParam String username, @RequestParam String notifyid, @RequestParam String sign) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        log.info("YB @RequestParam data: " + appid + "," + username + "," + notifyid + "," + sign);

        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("appid", appid);
        paramMap.put("username", username);
        paramMap.put("notifyid", notifyid);
        paramMap.put("sign", sign);

        log.info("YB map to json data: " + mapToJson(paramMap));

//        httpRequestLog.setRequestBody(params.toString());

        String traceId = httpRequestLog.getId();

        ResponseVo responseVo = new ResponseVo();

        try{
            String body = httpRequestLog.getRequestBody();

//            BalanceDto balanceDto = HttpService.convertJsonToDto(body, BalanceDto.class);

        }catch(Exception e){

        }finally{
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
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
}