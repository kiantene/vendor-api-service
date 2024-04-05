package com.nextgen.gameaggregator.vendor.ambslot.api.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.ambslot.vo.ResponseVo;
import lombok.Data;

@Data
public class BalanceVo extends ResponseVo {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DataVo data;
}
