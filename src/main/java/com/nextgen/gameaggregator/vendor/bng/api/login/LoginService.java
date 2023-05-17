package com.nextgen.gameaggregator.vendor.bng.api.login;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bng.vo.CommonVo;
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

    public CommonVo login(String body, String traceId) throws JsonProcessingException {

        // Retrieve request body in original string format
        LoginDto loginDto = HttpService.convertJsonToDto(body, LoginDto.class);

        // Construct VO
        LoginVo vo = new LoginVo();
        PlayerVo player = new PlayerVo();
        BalanceVo balance = new BalanceVo();

        player.setId("testgame3");
        player.setBrand("zt001winksw-stage");
        player.setCurrency("BRL");
        player.setMode("REAL");
        player.setIs_test(false);

        balance.setValue("0.00");
        long unixTime = System.currentTimeMillis(); //unix timestamp with millisecond
        balance.setVersion(BigInteger.valueOf(unixTime));

        vo.setUid(loginDto.getUid());
        vo.setPlayer(player);
        vo.setBalance(balance);
        vo.setTag("");

        return vo;
    }
}
