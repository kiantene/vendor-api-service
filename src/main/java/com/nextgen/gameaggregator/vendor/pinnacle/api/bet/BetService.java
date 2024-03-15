package com.nextgen.gameaggregator.vendor.pinnacle.api.bet;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsDto;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BetService {
    @Autowired
    private SportWalletService sportWalletService;

    public List<CommonVo> bet(ActionsDto dto, GameSession gameSession, HttpRequestLog httpRequestLog) {
        String traceId = httpRequestLog.getId();

        return dto.getActions().stream()
                .filter(action -> "BETTED".equals(action.getName()))
                .map(action -> {
                    CommonVo commonVo = new CommonVo();
                    commonVo.setId(action.getId());
                    commonVo.setTransactionId(action.getTransaction().getTransactionId());
                    commonVo.setWagerId(action.getWagerInfo().getWagerId());

                    try {
                        action.getWagerInfo().setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
                        BetEvent response = sportWalletService.placeBet(traceId, gameSession, action.getWagerInfo(), httpRequestLog.getRequestBody(), httpRequestLog);
                        commonVo.setResponseCode(ResponseCode.SUCCESS.code);
                        commonVo.setBalance(response.getLastBalance());

                    } catch (Exception e) {
                        log.error("Exception while placing bet: {}", e.getMessage());
                        commonVo.setResponseCode(ResponseCode.UNKNOWN_ERROR.code);
                    }

                    return commonVo;
                })
                .collect(Collectors.toList());
    }
}
