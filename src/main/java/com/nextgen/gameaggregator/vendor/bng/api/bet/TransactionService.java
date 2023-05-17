package com.nextgen.gameaggregator.vendor.bng.api.bet;

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
public class TransactionService {

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

    public CommonVo transaction(String body, String traceId) throws JsonProcessingException {

        // Retrieve request body in original string format
        TransactionDto transactionDto = HttpService.convertJsonToDto(body, TransactionDto.class);

        // Construct VO
        TransactionVo vo = new TransactionVo();
        BalanceVo balanceVo = new BalanceVo();

        balanceVo.setValue("0.00");
        long unixTime = System.currentTimeMillis(); //unix timestamp with millisecond
        balanceVo.setVersion(BigInteger.valueOf(unixTime));

        vo.setUid(transactionDto.getUid());
        vo.setBalance(balanceVo);

        return vo;
    }
}
