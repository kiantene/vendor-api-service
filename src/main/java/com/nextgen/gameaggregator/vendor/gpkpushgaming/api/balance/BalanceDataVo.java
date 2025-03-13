package com.nextgen.gameaggregator.vendor.gpkpushgaming.api.balance;

import com.nextgen.gameaggregator.vendor.gpkpushgaming.vo.DataVo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BalanceDataVo extends DataVo {
    private String user;

    private String cash;

    private String timestamp;
}
