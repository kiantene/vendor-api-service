package com.nextgen.gameaggregator.vendor.evolutionlive.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DebitCreditCancelDto extends BasicDto {
    private String currency;
    private GameDto game;
    private TransactionDto transaction;
}
