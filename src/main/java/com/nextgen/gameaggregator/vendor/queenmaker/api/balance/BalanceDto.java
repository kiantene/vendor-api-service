package com.nextgen.gameaggregator.vendor.queenmaker.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto {
    private String testmode;
    private List<User> users;
}
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class User {
    private String authtoken;
    private String userid;
    private String brandcode;
    private String lang;
    private String cur;
    private String walletcode;
}
