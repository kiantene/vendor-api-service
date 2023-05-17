package com.nextgen.gameaggregator.vendor.bng.api.balance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bng.api.login.LoginDto;
import com.nextgen.gameaggregator.vendor.bng.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public CommonVo balance(String body, String traceId) throws JsonProcessingException {

        // Retrieve request body in original string format
        BalanceDto balanceDto = HttpService.convertJsonToDto(body, BalanceDto.class);

        // Construct VO
        BalanceVo vo = new BalanceVo();
        AmountVo amountVo = new AmountVo();

        amountVo.setValue("0.00");
        long unixTime = System.currentTimeMillis(); //unix timestamp with millisecond
        amountVo.setVersion(BigInteger.valueOf(unixTime));

        vo.setUid(balanceDto.getUid());
        vo.setBalance(amountVo);

        return vo;
    }
}
