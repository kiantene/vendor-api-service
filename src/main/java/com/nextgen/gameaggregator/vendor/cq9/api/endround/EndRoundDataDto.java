package com.nextgen.gameaggregator.vendor.cq9.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndRoundDataDto {
    private String mtcode;
    private BigDecimal amount;
    private String eventtime;
}
