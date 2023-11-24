package com.nextgen.gameaggregator.vendor.pinnacle.api.bet;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsDto;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.CommonVo;

import lombok.extern.slf4j.Slf4j;

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

                    try {
                        sportWalletService.placeBet(traceId, gameSession, action.getWagerInfo(), httpRequestLog.getRequestBody(), httpRequestLog);
                    } catch (Exception e) {
                        log.error("Exception: " + e.getMessage());
                    }

                    CommonVo commonVo = new CommonVo();
                    commonVo.setId(action.getId());
                    commonVo.setTransactionId(action.getTransaction().getTransactionId());
                    commonVo.setWagerId(action.getWagerInfo().getWagerId());
                    commonVo.setResponseCode(ResponseCode.SUCCESS.code);
                    return commonVo;

                })
                .collect(Collectors.toList());
    }
}
