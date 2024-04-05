package com.nextgen.gameaggregator.vendor.evoplay.api.balanceIncrease;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.vendor.evoplay.dto.CallbackDto;
import com.nextgen.gameaggregator.vendor.evoplay.vo.ResponseDataVo;
import com.nextgen.gameaggregator.vendor.evoplay.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class BalanceIncreaseService {

    @Autowired
    private BalanceService balanceService;

    public ResponseVo balanceIncrease(CallbackDto callbackDto, GameSession gameSession, String traceId, HttpRequestLog httpRequestLog) {

        BigDecimal balance = BigDecimal.ZERO;

        try {
            BigDecimal currentBalance = balanceService.getBalance(gameSession.getVendorPlayerUsername(), traceId, gameSession, httpRequestLog);
            balance = balanceService.increaseBalance(gameSession.getVendorPlayerUsername(), currentBalance, new BigDecimal(callbackDto.getData().getAmount()));

        } catch (Exception exception) {
            log.error(exception.getMessage());
        }

        ResponseDataVo responseDataVo = new ResponseDataVo();
        responseDataVo.setBalance(balance);
        responseDataVo.setCurrency(gameSession.getVendorCurrencyCode());

        ResponseVo responseVo = new ResponseVo();
        responseVo.setData(responseDataVo);

        return responseVo;
    }
}
