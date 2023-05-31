package com.nextgen.gameaggregator.vendor.bng.api.bet;

import com.nextgen.gameaggregator.vendor.bng.vo.ErrorVo;
import lombok.Data;
import com.nextgen.gameaggregator.vendor.bng.vo.CommonVo;

@Data
public class TransactionVo extends CommonVo {

    private TransactionBalanceVo balance;
    private ErrorVo error;
}
