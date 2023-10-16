package com.nextgen.gameaggregator.vendor.saba.api.balance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.saba.dto.RequestDto;
import com.nextgen.gameaggregator.vendor.saba.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class GetBalanceAction {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private WalletService walletService;

    @PostMapping(path = EndPoints.GET_BALANCE)
    public GetBalanceVo action(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct Vo
        GetBalanceVo vo = new GetBalanceVo();

        try {
            // Convert original request body into dto
            RequestDto<GetBalanceDto> dto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), new TypeReference<>() {});

            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getMessage().getUserId());

            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            vo.setStatus("0");
            vo.setUserId(dto.getMessage().getUserId());
            vo.setBalance(balance);
            vo.setBalanceTs(vendorService.convertDateTimeFormat(System.currentTimeMillis()));

        } catch (Exception e) {
            vo.setStatus("999");
            vo.setMsg("System Error");
            httpService.logError(httpRequestLog, e);

        } finally {
            httpService.end(httpRequestLog, vo);

        }

        return vo;
    }
}
