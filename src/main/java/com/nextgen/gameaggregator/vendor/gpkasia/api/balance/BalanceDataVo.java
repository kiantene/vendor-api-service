package com.nextgen.gameaggregator.vendor.gpkasia.api.balance;

import com.nextgen.gameaggregator.vendor.gpkasia.vo.DataVo;
import lombok.Data;

@Data
public class BalanceDataVo extends DataVo {
    private String user;

    private String cash;

    private String timestamp;
}
