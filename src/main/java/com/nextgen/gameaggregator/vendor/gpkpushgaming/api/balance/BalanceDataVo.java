package com.nextgen.gameaggregator.vendor.gpkpushgaming.api.balance;

import com.nextgen.gameaggregator.vendor.gpkpushgaming.vo.DataVo;
import lombok.Data;

@Data
public class BalanceDataVo extends DataVo {
    private String user;

    private String cash;

    private String timestamp;
}
