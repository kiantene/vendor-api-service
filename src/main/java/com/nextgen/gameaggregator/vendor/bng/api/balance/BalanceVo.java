package com.nextgen.gameaggregator.vendor.bng.api.balance;

import com.nextgen.gameaggregator.vendor.bng.vo.CommonVo;
import lombok.Data;

@Data
public class BalanceVo extends CommonVo {
    private String uid;
    private AmountVo balance;
}
