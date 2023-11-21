package com.nextgen.gameaggregator.vendor.queenmaker.api.reward;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RewardDto {
    private String brandcode;
    private List<RewardTransactionsDto> transactions;
}
