package com.nextgen.gameaggregator.vendor.cq9.api.balance;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.cq9.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cq9.constant.Formats;
import com.nextgen.gameaggregator.vendor.cq9.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.cq9.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.cq9.vo.StatusVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping(path = EndPoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
public class BalanceAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;

    @GetMapping(path = EndPoints.BALANCE)
    public ResponseVo<BalanceVo> balance(@PathVariable("account") String account, HttpServletRequest request) {
        // Construct Vo
        ResponseVo<BalanceVo> responseVo = new ResponseVo<>();
        StatusVo statusVo = new StatusVo();
        responseVo.setStatus(statusVo);

        HttpRequestLog httpRequestLog = httpService.logRequest(request);
        BalanceVo balanceVo = new BalanceVo();
        String traceId = UUID.randomUUID().toString();

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            BalanceDto balanceDto = HttpService.convertQueryStringToDto(body, BalanceDto.class);

            balanceVo.setBalance(BigDecimal.valueOf(100));
            balanceVo.setCurrency("CNY");

            responseVo.setData(balanceVo);

        } catch (Exception exception) { // any other exception encountered
            statusVo.setCode(ResponseCodes.SERVER_ERROR);
        } finally {
            if (!statusVo.getCode().equals("0")) {
                httpRequestLog.setStatus(HttpService.ERROR);
            }
            statusVo.setMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(statusVo.getCode()));
            statusVo.setDateTime(new SimpleDateFormat(Formats.DATE_TIME_FORMAT).format(new Date()));
            httpRequestLog.setEndTime(System.currentTimeMillis());
            ConcurrencyService.THREAD_POOL.submit(() -> httpService.logResponse(httpRequestLog, responseVo, traceId));
        }

        return responseVo;
    }
}
