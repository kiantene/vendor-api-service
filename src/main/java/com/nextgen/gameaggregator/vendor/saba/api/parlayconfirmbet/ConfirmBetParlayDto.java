package com.nextgen.gameaggregator.vendor.saba.api.parlayconfirmbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.saba.dto.GeneralDto;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfirmBetParlayDto extends GeneralDto {

    private String userId;
    private String operationId;
    private String updateTime;
    private BigDecimal creditAmount;
    private BigDecimal debitAmount;
    private String transactionTime;
    private List<ConfirmBetParlayTxnsDto> txns;
}
