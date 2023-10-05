package com.nextgen.gameaggregator.vendor.saba.api.cancelbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.saba.dto.TransactionDto;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelBetDto {
    private String action;
    private String operationId;
    private String userId;
    private String updateTime;
    private List<TransactionDto> txns;
}
