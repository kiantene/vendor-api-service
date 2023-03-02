package com.nextgen.gameaggregator.vendor.facai.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto {
    public String MemberAccount;
    public String Currency;
    public Integer GameID;
    public Long Ts;
}
