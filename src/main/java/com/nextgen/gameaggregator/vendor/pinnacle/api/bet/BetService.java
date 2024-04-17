package com.nextgen.gameaggregator.vendor.pinnacle.api.bet;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.Action;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsTransactionDto;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsWagerInfoDto;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class BetService {
    @Autowired
    private HttpService httpService;
    @Autowired
    private SportWalletService sportWalletService;

    public CommonVo bet(Action action, GameSession gameSession, HttpRequestLog httpRequestLog) {
        String traceId = httpRequestLog.getId();
        Long transactionId = Optional.ofNullable(action.getTransaction()).map(ActionsTransactionDto::getTransactionId).orElse(null);
        Long wagerId = Optional.ofNullable(action.getWagerInfo()).map(ActionsWagerInfoDto::getWagerId).orElse(null);
        CommonVo commonVo = new CommonVo(action.getId(), transactionId, wagerId);

        try {
            BetDto betDto = new ModelMapper().map(action.getWagerInfo(), BetDto.class);
            betDto.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
            betDto.setTransactionDate(Optional.ofNullable(action.getTransaction()).map(ActionsTransactionDto::getTransactionDate).orElse(null));
            betDto.setExternalTransactionId(action.getId().toString());
            BetEvent response = sportWalletService.placeBet(traceId, gameSession, betDto, httpRequestLog.getRequestBody(), httpRequestLog);
            commonVo.setBalance(response.getLastBalance());

        } catch (InsufficientBalanceException e) {
            httpService.logError(httpRequestLog, e);
            commonVo.setResponseCode(ResponseCode.INSUFFICIENT_FUND.code);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            commonVo.setResponseCode(ResponseCode.UNKNOWN_ERROR.code);
        }

        return commonVo;
    }
}
