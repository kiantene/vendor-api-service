package com.nextgen.gameaggregator.vendor.queenmaker.api.balance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Formats;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
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
    public HttpResponse balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        BalanceVo balanceVo = new BalanceVo();
        String traceId = httpRequestLog.getTraceId();




        try {
            String clientId = request.getHeader(Formats.HEADER_CLIENT_ID);
            String clientSecret = request.getHeader(Formats.HEADER_CLIENT_SECRET);
            String body = httpRequestLog.getRequestBody();

            ObjectMapper objectMapper = new ObjectMapper();
            BalanceDto balanceDto = objectMapper.readValue(body, BalanceDto.class);
            List<UsersVo> usersList = new LinkedList<>();



            for(UsersDto user : balanceDto.getUsers()) {
//                GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(user.getUserid());

                // Set wallet for each user
                WalletsVo walletVo = new WalletsVo();
                walletVo.setCode("MainWallet");
                walletVo.setBal(BigDecimal.valueOf(100));
                walletVo.setCur(user.getCur());

                UsersVo usersVo = new UsersVo();
                usersVo.setUserid(user.getUserid());
                List<WalletsVo> walletsList = new LinkedList<>();
                walletsList.add(walletVo);
                usersVo.setWallets(walletsList);
                usersList.add(usersVo);
            }
            balanceVo.setUsers(usersList);

        } catch (Exception exception) { // any other exception encountered

            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, balanceVo);
        }

        return balanceVo;
    }

}
