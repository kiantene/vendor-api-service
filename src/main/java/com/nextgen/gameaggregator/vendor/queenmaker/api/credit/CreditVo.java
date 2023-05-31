package com.nextgen.gameaggregator.vendor.queenmaker.api.credit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.queenmaker.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.queenmaker.vo.TransactionsVo;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreditVo extends ResponseVo {
    private List<TransactionsVo> transactions;

    @Override
    public boolean hasError() {
        return false;
    }
}
