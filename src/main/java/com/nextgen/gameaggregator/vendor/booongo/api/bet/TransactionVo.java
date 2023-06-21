package com.nextgen.gameaggregator.vendor.booongo.api.bet;

import lombok.Data;
import com.nextgen.gameaggregator.vendor.booongo.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.booongo.vo.BalanceVo;

@Data
public class TransactionVo extends CommonVo {

    private BalanceVo balance;
}
