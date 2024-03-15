package com.nextgen.gameaggregator.vendor.pinnacle.api.settled;

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
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SettledService {
    @Autowired
    private SportWalletService sportWalletService;

    public List<CommonVo> settled(ActionsDto dto, HttpRequestLog httpRequestLog) {

        return dto.getActions().stream()
                .filter(action -> "SETTLED".equals(action.getName()))
                .map(action -> {
                    CommonVo commonVo = new CommonVo();
                    commonVo.setId(action.getId());
                    commonVo.setWagerId(action.getWagerInfo().getWagerId());

                    try {
                        String traceId = UUID.randomUUID().toString();
                        action.getWagerInfo().setVendorPlayerUsername(action.getPlayerInfo().getUserCode());
                        BetEvent response = sportWalletService.settle(traceId, action.getWagerInfo(), httpRequestLog);
                        commonVo.setResponseCode(ResponseCode.SUCCESS.code);
                        commonVo.setBalance(response.getLastBalance());

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
