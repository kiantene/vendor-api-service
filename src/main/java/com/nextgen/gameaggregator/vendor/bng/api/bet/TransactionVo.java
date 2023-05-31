package com.nextgen.gameaggregator.vendor.bng.api.bet;

import lombok.Data;
import com.nextgen.gameaggregator.vendor.bng.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.bng.vo.BalanceVo;

@Data
public class TransactionVo extends CommonVo {

    private BalanceVo balance;
}
