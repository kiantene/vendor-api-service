package com.nextgen.gameaggregator.vendor.gpkpushgaming.api.rollback;

import com.nextgen.gameaggregator.vendor.gpkpushgaming.vo.DataVo;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RollBackDataVo extends DataVo {
    private BigDecimal money;

    private String timestamp;

    private String dealid;

    private String cash;
}
