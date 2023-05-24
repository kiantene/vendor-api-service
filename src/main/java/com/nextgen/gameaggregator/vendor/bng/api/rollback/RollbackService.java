package com.nextgen.gameaggregator.vendor.bng.api.rollback;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.bng.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.cq9.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

@Service
@Slf4j
public class RollbackService {

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

    public CommonVo rollback(HttpRequestLog httpRequestLog, String traceId) {

        // Construct vo
        RollbackVo vo = new RollbackVo();
        RollbackBalanceVo rollbackBalanceVo = new RollbackBalanceVo();

        try{
            // Retrieve request body in original string format
            RollbackDto rollbackDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), RollbackDto.class);

            // Verify session token
            GameSession gameSession = gameSessionService.verifyToken(rollbackDto.getToken());

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.processRollback(traceId, rollbackDto, gameSession, vendorService);

            long unixTime = System.currentTimeMillis(); //unix timestamp with millisecond

            // Construct response data into vo
            rollbackBalanceVo.setValue(balance.setScale(2, RoundingMode.DOWN).toString());
            rollbackBalanceVo.setVersion(BigInteger.valueOf(unixTime));

            vo.setUid(rollbackDto.getUid());
            vo.setBalance(rollbackBalanceVo);

        }catch(Exception exception){

        }

        return vo;
    }
}
