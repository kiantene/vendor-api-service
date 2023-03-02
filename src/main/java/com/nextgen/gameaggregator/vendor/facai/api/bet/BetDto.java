package com.nextgen.gameaggregator.vendor.facai.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.pojava.datetime.DateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto {

    public String RecordID;
    public Long BankID;
    public String MemberAccount;
    public String Currency;
    public Integer GameID;
    public Integer GameType;
    public Boolean isBuyFeature;
    public Double Bet;
    public Double Win;
    public Double JPBet;
    public Double JPPrize;
    public Double NetWin;
    public Double RequireAmt;
    public DateTime GameDate;
    public DateTime CreateDate;
    public Long Ts;

}
