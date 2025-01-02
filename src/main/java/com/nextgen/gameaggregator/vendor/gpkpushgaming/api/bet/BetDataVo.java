package com.nextgen.gameaggregator.vendor.gpkpushgaming.api.bet;

import com.nextgen.gameaggregator.vendor.gpkpushgaming.vo.DataVo;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BetDataVo extends DataVo {
    private BigDecimal money;

    private String timestamp;

    private String dealid;

    private String cash;
}
