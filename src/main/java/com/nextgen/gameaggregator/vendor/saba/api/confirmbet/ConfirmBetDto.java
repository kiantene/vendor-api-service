package com.nextgen.gameaggregator.vendor.saba.api.confirmbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.saba.dto.TransactionDto;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfirmBetDto {
    private String action;
    private String operationId;
    private String userId;
    private String updateTime;
    private String transactionTime;
    private List<TransactionDto> txns;
}
