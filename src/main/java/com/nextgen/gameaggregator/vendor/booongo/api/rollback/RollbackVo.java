package com.nextgen.gameaggregator.vendor.booongo.api.rollback;

import com.nextgen.gameaggregator.vendor.booongo.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.booongo.vo.BalanceVo;
import lombok.Data;

@Data
public class RollbackVo extends CommonVo {

    private BalanceVo balance;
}
