package com.nextgen.gameaggregator.vendor.saba.api.parlaybet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaceBetParlayTxnDetailDto {
    private BigDecimal odds;
}
