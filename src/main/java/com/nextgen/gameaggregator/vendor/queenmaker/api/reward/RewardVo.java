package com.nextgen.gameaggregator.vendor.queenmaker.api.reward;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.queenmaker.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.queenmaker.vo.TransactionsVo;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RewardVo extends ResponseVo {
    private List<TransactionsVo> transactions;
}
