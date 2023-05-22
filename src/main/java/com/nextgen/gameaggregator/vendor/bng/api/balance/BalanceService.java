package com.nextgen.gameaggregator.vendor.bng.api.balance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.bng.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;

@Service
@Slf4j
public class BalanceService {
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

    public CommonVo balance(HttpRequestLog httpRequestLog, String traceId) {

        // Construct VO
        BalanceVo vo = new BalanceVo();
        BalanceAmountVo balanceAmountVo = new BalanceAmountVo();

        try {
            // Retrieve request body in original string format
            BalanceDto balanceDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), BalanceDto.class);

            // Verify session token
            GameSession gameSession = gameSessionService.verifyToken(balanceDto.getToken());

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            long unixTime = System.currentTimeMillis(); //unix timestamp with millisecond

            balanceAmountVo.setValue(balance.toString());
            balanceAmountVo.setVersion(BigInteger.valueOf(unixTime));

            vo.setUid(balanceDto.getUid());
            vo.setBalance(balanceAmountVo);

        } catch (Exception exception) {

        }

        return vo;
    }
}
