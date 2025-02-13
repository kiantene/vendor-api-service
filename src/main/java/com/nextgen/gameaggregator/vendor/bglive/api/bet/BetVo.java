package com.nextgen.gameaggregator.vendor.bglive.api.bet;

import com.nextgen.gameaggregator.vendor.bglive.vo.CommonVo;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BetVo extends CommonVo {
    private Long userId;
    private String sn;
    private BigDecimal amount;
    private String orderResult;
    private String tranId;
}