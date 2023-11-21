package com.nextgen.gameaggregator.vendor.pinnacle.api.bet;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsDto;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.CommonVo;

@Service
public class BetService {
    public List<CommonVo> bet(ActionsDto dto, HttpRequestLog httpRequestLog) {
        return dto.getActions().stream()
                .filter(action -> "BETTED".equals(action.getName()))
                .map(action -> {
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
