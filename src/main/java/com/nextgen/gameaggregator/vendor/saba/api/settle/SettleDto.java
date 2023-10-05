package com.nextgen.gameaggregator.vendor.saba.api.settle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.saba.dto.TransactionDto;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettleDto {
    private String action;
    private String operationId;
    private List<TransactionDto> txns;
}
