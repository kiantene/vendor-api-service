package com.nextgen.gameaggregator.vendor.saba.api.parlaybet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.saba.dto.GeneralDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaceBetParlayDto extends GeneralDto {
    private String operationId;
    private String userId;
    private Integer currency;
    private String betTime;
    private String updateTime;
    private BigDecimal totalBetAmount;
    private String IP;
    private String tsId;
    private String betFrom;
    private BigDecimal creditAmount;
    private BigDecimal debitAmount;
    private String vendorTransId;
    private List<PlaceBetParlayTxnsDto> txns;
    private List<String> ticketDetail;
}
