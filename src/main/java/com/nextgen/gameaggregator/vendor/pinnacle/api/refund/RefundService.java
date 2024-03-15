package com.nextgen.gameaggregator.vendor.pinnacle.api.refund;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsDto;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsWagerInfoDto;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RefundService {
    @Autowired
    private SportWalletService sportWalletService;

    public List<CommonVo> refund(ActionsDto dto, HttpRequestLog httpRequestLog) {
        String traceId = httpRequestLog.getId();

        return dto.getActions().stream()
                .filter(action -> {
                    String actionName = action.getName();
                    return "REJECTED".equals(actionName) || "ROLLBACKED".equals(actionName) || "CANCELLED".equals(actionName);
                })
                .map(action -> {
                    CommonVo commonVo = new CommonVo();
                    commonVo.setId(action.getId());
                    commonVo.setWagerId(action.getWagerInfo().getWagerId());

                    try {
                        ActionsWagerInfoDto actionsWagerInfoDto = action.getWagerInfo();
                        actionsWagerInfoDto.setVendorPlayerUsername(action.getPlayerInfo().getUserCode());
                        BetEvent response = sportWalletService.refund(traceId, actionsWagerInfoDto, httpRequestLog.getRequestBody(), httpRequestLog);
                        commonVo.setResponseCode(ResponseCode.SUCCESS.code);
                        commonVo.setBalance(response.getLastBalance());

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
