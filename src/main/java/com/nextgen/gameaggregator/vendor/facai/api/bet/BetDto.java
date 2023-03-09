package com.nextgen.gameaggregator.vendor.facai.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.pojava.datetime.DateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto {

    @JsonProperty("RecordID")
    public String recordID;
    @JsonProperty("BankID")
    public Long bankID;
    @JsonProperty("MemberAccount")
    public String memberAccount;
    @JsonProperty("Currency")
    public String currency;
    @JsonProperty("GameID")
    public Integer gameID;
    @JsonProperty("GameType")
    public Integer gameType;
    @JsonProperty("isBuyFeature")
    public Boolean isBuyFeature;
    @JsonProperty("Bet")
    public Double bet;
    @JsonProperty("Win")
    public Double win;
    @JsonProperty("JPBet")
    public Double jpBet;
    @JsonProperty("JPPrize")
    public Double JpPrize;
    @JsonProperty("NetWin")
    public Double netWin;
    @JsonProperty("RequireAmt")
    public Double requireAmt;
    @JsonProperty("GameDate")
    public DateTime gameDate;
    @JsonProperty("CreateDate")
    public DateTime createDate;
    @JsonProperty("Ts")
    public Long ts;

}
