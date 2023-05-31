package com.nextgen.gameaggregator.vendor.bng.api.rollback;

import com.nextgen.gameaggregator.vendor.bng.vo.CommonVo;
import lombok.Data;

@Data
public class RollbackVo extends CommonVo {

    private RollbackBalanceVo balance;
}
