package com.nextgen.gameaggregator.vendor.gpkasia.api.bet;

import com.nextgen.gameaggregator.vendor.gpkasia.vo.DataVo;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BetDataVo extends DataVo {
    private BigDecimal money;

    private String timestamp;

    private String dealid;

    private String cash;
}
