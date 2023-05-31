package com.nextgen.gameaggregator.vendor.bng.api.rollback;

import com.nextgen.gameaggregator.vendor.bng.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.bng.vo.BalanceVo;
import lombok.Data;

@Data
public class RollbackVo extends CommonVo {

    private BalanceVo balance;
}
