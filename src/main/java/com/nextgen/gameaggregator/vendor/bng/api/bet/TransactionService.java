package com.nextgen.gameaggregator.vendor.bng.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.bng.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    public CommonVo transaction(String body, String traceId){

        // Construct VO
        TransactionVo vo = new TransactionVo();
        TransactionBalanceVo transactionBalanceVo = new TransactionBalanceVo();

        try{
            // Retrieve request body in original string format
            TransactionDto transactionDto = HttpService.convertJsonToDto(body, TransactionDto.class);

            // Verify session token
            GameSession gameSession = gameSessionService.verifyToken(transactionDto.getToken());

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            long unixTime = System.currentTimeMillis(); //unix timestamp with millisecond

            transactionBalanceVo.setValue(balance.toString());
            transactionBalanceVo.setVersion(BigInteger.valueOf(unixTime));

            vo.setUid(transactionDto.getUid());
            vo.setBalance(transactionBalanceVo);

        }catch(Exception exception){

        }

        return vo;
    }
}
