package com.nextgen.gameaggregator.vendor.facai.api.bet;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.facai.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.facai.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.facai.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BetAction {

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
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.SLOT_BET)
    public BetVo bet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();

        // Construct VO
        BetVo betVo = new BetVo();
        betVo.setResult(0);
        betVo.setMainPoints(1000.00);

        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into commonDto
            CommonDto commonDto = HttpService.convertQueryStringToDtoUrlDecode(body, CommonDto.class);

            //TODO pending PG update core function to get appKey
            //Decrypt raw respond
            String jsonParam = vendorService.aesDecrypt(commonDto.getParams(), "Q7RaR8CUbwZ0roD2");

            //map decrypted data(string json) into balanceDto
            BetDto betDto = HttpService.convertJsonToDto(jsonParam, BetDto.class);

        }catch (Exception exception) {
        }finally {
            httpService.end(httpRequestLog, betVo);
        }



        return betVo;
    }
}
