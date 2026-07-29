package com.nextgen.gameaggregator.vendor.evoplay.api.v2.balanceIncrease;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.vo.ResponseDataVo;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.evoplay.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.CallbackDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class BalanceIncreaseService {

    private final BalanceService balanceService;
    private final GameSessionService gameSessionService;

    public BalanceIncreaseService(BalanceService balanceService, GameSessionService gameSessionService) {
        this.balanceService = balanceService;
        this.gameSessionService = gameSessionService;
    }

    public ResponseVo balanceIncrease(CallbackDto callbackDto, String traceId, HttpRequestLog httpRequestLog) {

        BigDecimal balance = BigDecimal.ZERO;
        GameSession gameSession = gameSessionService.getLastGameSessionByVendorPlayerUsername(callbackDto.getUsername());
        try {
            BigDecimal currentBalance = balanceService.getBalance(gameSession.getVendorPlayerUsername(), traceId, gameSession, httpRequestLog);
            balance = balanceService.increaseBalance(gameSession.getVendorPlayerUsername(), currentBalance, new BigDecimal(callbackDto.getData().getAmount()));

        } catch (Exception exception) {
            log.error(exception.getMessage());
        }

        return ResponseVo.builder()
                .status(ResponseCodes.SUCCESS.status)
                .data(mapResponseData(balance, gameSession.getVendorCurrencyCode()))
                .build();

    }

    private ResponseDataVo mapResponseData(BigDecimal balance, String currency) {
        return ResponseDataVo.builder()
                .balance(balance)
                .currency(currency)
                .build();
    }
}
