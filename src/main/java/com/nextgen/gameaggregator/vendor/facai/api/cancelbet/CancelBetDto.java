package com.nextgen.gameaggregator.vendor.facai.api.cancelbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelBetDto {

    public Long BankID;
    public String Currency;
    public String MemberAccount;
    public Integer GameID;
    public Long Ts;

}
