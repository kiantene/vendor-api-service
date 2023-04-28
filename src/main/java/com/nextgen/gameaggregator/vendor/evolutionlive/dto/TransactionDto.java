package com.nextgen.gameaggregator.vendor.evolutionlive.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionDto {
    private String id;
    private String refId;
    private BigDecimal amount;
}
