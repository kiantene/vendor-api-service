package com.nextgen.gameaggregator.vendor.pinnacle.api.settled;

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
public class SettledService {
    @Autowired
    private SportWalletService sportWalletService;

    public List<CommonVo> settled(ActionsDto dto, GameSession gameSession, HttpRequestLog httpRequestLog) {

        return dto.getActions().stream()
                .filter(action -> "SETTLED".equals(action.getName()))
                .map(action -> {
                    CommonVo commonVo = new CommonVo();
                    commonVo.setId(action.getId());
                    commonVo.setWagerId(action.getWagerInfo().getWagerId());

                    try {
                        action.getWagerInfo().setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
                        sportWalletService.settle(action.getWagerInfo(), httpRequestLog);
                        commonVo.setResponseCode(ResponseCode.SUCCESS.code);

                    } catch (Exception e) {
                        log.error("Exception while settling bet: {}", e.getMessage());
                        commonVo.setResponseCode(ResponseCode.UNKNOWN_ERROR.code);
                    }

                    if (action.getTransaction() != null) {
                        commonVo.setTransactionId(action.getTransaction().getTransactionId());
                    }
                    
                    commonVo.setWagerId(action.getWagerInfo().getWagerId());
                    return commonVo;
                })
                .collect(Collectors.toList());
    }
}
