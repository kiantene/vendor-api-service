package com.nextgen.gameaggregator.vendor.cq9.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto {
    private String account;
    private String eventTime;
    private String gamehall;
    private String gamecode;
    private String roundid;
    private BigDecimal amount;
    private String mtcode;
    private String session;
    private String platform;
}
