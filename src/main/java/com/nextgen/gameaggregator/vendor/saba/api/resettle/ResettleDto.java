package com.nextgen.gameaggregator.vendor.saba.api.resettle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResettleDto {
    private String action;
    private String operationId;
    private List<ResettleTransactionDto> txns;
}
