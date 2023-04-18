package com.nextgen.gameaggregator.vendor.queenmaker.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UsersDto {
    private String authtoken;
    private String userid;
    private String brandcode;
    private String lang;
    private String cur;
    private String walletcode;
}
