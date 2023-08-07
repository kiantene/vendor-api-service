package com.nextgen.gameaggregator.vendor.booongo.api.freespin;

import com.nextgen.gameaggregator.vendor.booongo.vo.BalanceVo;
import com.nextgen.gameaggregator.vendor.booongo.vo.CommonVo;
import lombok.Data;

@Data
public class FreeSpinVo extends CommonVo {

    private BalanceVo balance;
}
