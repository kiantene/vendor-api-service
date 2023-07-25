package com.nextgen.gameaggregator.vendor.booongo.api.balance;

import com.nextgen.gameaggregator.vendor.booongo.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.booongo.vo.BalanceVo;
import lombok.Data;

@Data
public class BalanceServiceVo extends CommonVo {

    private BalanceVo balance;
}
