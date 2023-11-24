package com.nextgen.gameaggregator.vendor.saba.api.unsettle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.saba.api.confirmbet.ConfirmBetTransactionDto;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UnsettleDto {
    private String action;
    private String operationId;
    private List<ConfirmBetTransactionDto> txns;
}
