package com.nextgen.gameaggregator.vendor.dotconnections.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ResponseDataVo {
    private String brandUid;
    private String currency;
    private BigDecimal balance;
}