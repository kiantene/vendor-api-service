package com.nextgen.gameaggregator.vendor.bng.api.balance;

import com.nextgen.gameaggregator.vendor.bng.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.bng.vo.BalanceVo;
import lombok.Data;

@Data
public class BalanceServiceVo extends CommonVo {

    private BalanceVo balance;
}
