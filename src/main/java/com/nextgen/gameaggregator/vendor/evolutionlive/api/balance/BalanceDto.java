package com.nextgen.gameaggregator.vendor.evolutionlive.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.evolutionlive.dto.BasicDto;
import com.nextgen.gameaggregator.vendor.evolutionlive.dto.GameDto;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto extends BasicDto {
    private GameDto game;
    private String currency;

}