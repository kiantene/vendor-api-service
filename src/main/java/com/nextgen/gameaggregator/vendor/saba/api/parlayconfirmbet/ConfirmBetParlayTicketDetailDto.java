package com.nextgen.gameaggregator.vendor.saba.api.parlayconfirmbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfirmBetParlayTicketDetailDto {
    private BigDecimal odds;
    private Integer oddsType;
}
