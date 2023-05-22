package com.nextgen.gameaggregator.vendor.bng.api.login;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.bng.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.bng.constant.Credentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;

@Service
@Slf4j
public class LoginService {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private HttpService httpService;

    public CommonVo login(HttpRequestLog httpRequestLog, String traceId) {

        // Construct VO
        LoginVo vo = new LoginVo();
        LoginPlayerVo loginPlayer = new LoginPlayerVo();
        LoginBalanceVo loginBalance = new LoginBalanceVo();

        try {

            // Retrieve request body in original string format
            LoginDto loginDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), LoginDto.class);

            // Verify session token
            GameSession gameSession = gameSessionService.verifyToken(loginDto.getToken());

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            // Retrieve vendor line credentials
            String brand = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PROJECT_NAME);

            loginPlayer.setId(gameSession.getVendorPlayerUsername());
            loginPlayer.setBrand(brand);
            loginPlayer.setCurrency(gameSession.getVendorCurrencyCode());
            loginPlayer.setMode("REAL"); // "FUN" or "REAL", REAL by default. Mode of the player. 'FUN' stands for playing for fun not using real funds, 'REAL' stands for playing using real funds
            loginPlayer.setIs_test(false); // 'false' meant players are a subject for invoicing at production environment!

            long unixTime = System.currentTimeMillis(); //unix timestamp with millisecond

            loginBalance.setValue(balance.toString());
            loginBalance.setVersion(BigInteger.valueOf(unixTime));

            vo.setUid(loginDto.getUid());
            vo.setPlayer(loginPlayer);
            vo.setBalance(loginBalance);
            vo.setTag("");

        } catch (Exception exception) {

        }

        return vo;
    }
}
