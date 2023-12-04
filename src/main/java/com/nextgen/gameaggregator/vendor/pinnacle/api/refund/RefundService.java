package com.nextgen.gameaggregator.vendor.pinnacle.api.refund;

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
public class RefundService {
    @Autowired
    private SportWalletService sportWalletService;

    public List<CommonVo> refund(ActionsDto dto, GameSession gameSession, HttpRequestLog httpRequestLog) {
        String traceId = httpRequestLog.getId();

        return dto.getActions().stream()
                .filter(action -> "REJECTED".equals(action.getName()))
                .map(action -> {
                    CommonVo commonVo = new CommonVo();
                    commonVo.setId(action.getId());

                    try {
                        sportWalletService.refund(traceId, gameSession, action.getWagerInfo(), httpRequestLog.getRequestBody(), httpRequestLog);
                        commonVo.setResponseCode(ResponseCode.SUCCESS.code);

                    } catch (Exception e) {
                        log.error("Exception while refunding bet: {}", e.getMessage());
                        commonVo.setResponseCode(ResponseCode.UNKNOWN_ERROR.code);
                    }

                    commonVo.setTransactionId(action.getTransaction().getTransactionId());
                    commonVo.setWagerId(action.getWagerInfo().getWagerId());
                    return commonVo;

                })
                .collect(Collectors.toList());
    }
}
