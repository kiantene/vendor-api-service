package com.nextgen.gameaggregator.vendor.gpkasia.api.bet;

import com.nextgen.gameaggregator.vendor.gpkasia.vo.DataVo;
import lombok.Data;

@Data
public class BetDataVo extends DataVo {
    private Double money;

    private String timestamp;

    private String dealid;

    private String cash;
}
