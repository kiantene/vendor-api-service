package com.nextgen.gameaggregator.vendor.kypoker.api.settle;

import com.nextgen.gameaggregator.vendor.kypoker.vo.ResponseObjectDto;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SettleVo extends ResponseObjectDto {
    private Integer roomMode;
    private Integer betCount;
    private BigDecimal totalBet;
    private BigDecimal validBet;
    private BigDecimal totalWithdraw;
    private BigDecimal revenue;
}
