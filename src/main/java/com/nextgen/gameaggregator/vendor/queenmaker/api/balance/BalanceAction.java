package com.nextgen.gameaggregator.vendor.queenmaker.api.balance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Formats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

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

    @PostMapping(path = EndPoints.WALLET_BALANCE)
    public Object balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        BalanceVo balanceVo = new BalanceVo();
        String traceId = httpRequestLog.getTraceId();

        HashMap<String, Object> response = new LinkedHashMap<>();
        List<Object> usersList = new LinkedList<>();
        List<Object> walletsList = new LinkedList<>();
        HashMap<String, Object> wallets = new LinkedHashMap<>();
        wallets.put("code", "MainWallet");
        wallets.put("bal", 1000);
        wallets.put("cur", "RMB");
        walletsList.add(wallets);

        try {
            String clientId = request.getHeader(Formats.HEADER_CLIENT_ID);
            String clientSecret = request.getHeader(Formats.HEADER_CLIENT_SECRET);
            String body = httpRequestLog.getRequestBody();

            ObjectMapper objectMapper = new ObjectMapper();
            BalanceDto balanceDto = objectMapper.readValue(body, BalanceDto.class);


            for(UsersDto user : balanceDto.getUsers()) {
                HashMap<String, Object> users = new LinkedHashMap<>();
                users.put("userid", user.getUserid());
                users.put("wallets", walletsList);
                usersList.add(users);
            }


            response.put("users", usersList);




        } catch (Exception exception) { // any other exception encountered

            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, balanceVo);
        }

        return response;
    }

}
