package com.nextgen.gameaggregator.vendor.queenmaker.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto {
    private String testmode;
    private List<UsersDto> users;
}

