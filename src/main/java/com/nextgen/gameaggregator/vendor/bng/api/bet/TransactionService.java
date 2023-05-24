package com.nextgen.gameaggregator.vendor.bng.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.bng.vo.ErrorVo;
import com.nextgen.gameaggregator.vendor.bng.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bng.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.bng.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

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
    @Autowired
    private VendorService vendorService;

    public CommonVo transaction(HttpRequestLog httpRequestLog, String traceId) {

        // Construct VO
        TransactionVo vo = new TransactionVo();
        TransactionBalanceVo transactionBalanceVo = new TransactionBalanceVo();
        ErrorVo errorVo = new ErrorVo();

        TransactionDto transactionDto = new TransactionDto();

        BigDecimal balance;

        long unixTime = System.currentTimeMillis(); //unix timestamp with millisecond

        GameSession gameSession = new GameSession();

        try {
            // Retrieve request body in original string format
            transactionDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), TransactionDto.class);

            // Verify session token
            gameSession = gameSessionService.verifyToken(transactionDto.getToken());

            ResultType resultType = getResultType(transactionDto);
            balance = walletService.processBetResult(traceId, gameSession, transactionDto, resultType, vendorService, httpRequestLog);

            transactionBalanceVo.setValue(balance.setScale(2, RoundingMode.DOWN).toString());

        }catch(InvalidOperatorResponseException invalidOperatorResponseException){ // If insufficient balance for placing bet

            errorVo.setCode(ResponseCodes.FUNDS_EXCEED);

            balance = getCurrentBalance(traceId,gameSession);

            // Retrieve current wallet balance
            transactionBalanceVo.setValue(balance.setScale(2, RoundingMode.DOWN).toString());

            vo.setError(errorVo);
        } catch (Exception exception) {
//            System.out.println(exception.getMessage());
        }finally {
            transactionBalanceVo.setVersion(BigInteger.valueOf(unixTime));
            vo.setUid(transactionDto.getUid());
            vo.setBalance(transactionBalanceVo);
        }

        return vo;
    }

    private ResultType getResultType(TransactionDto transactionDto) {

        ResultType resultType = ResultType.BET_LOSE; // Default value is lose
        BigDecimal zero = BigDecimal.ZERO;

        BigDecimal winAmount = new BigDecimal(transactionDto.getArgs().getWin());

        // If win amount is not equal to zero meant win(sometimes result in win but lose money)
        if (winAmount.compareTo(zero) > 0) { // Win amount greater than 0 ~ BET_WIN
            resultType = ResultType.BET_WIN;
        }

        return resultType;
    }

    private BigDecimal getCurrentBalance(String traceId, GameSession gameSession){

        BigDecimal balance = BigDecimal.ZERO;

        try{
            balance = walletService.getBalance(traceId, gameSession);

        }catch(Exception exception){

        }

        return balance;
    }
}
