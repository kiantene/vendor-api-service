package com.nextgen.gameaggregator.vendor.facai.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VendorBetDto {

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
    public BigDecimal bet;
    @JsonProperty("Win")
    public BigDecimal win;
    @JsonProperty("jpBet")
    public BigDecimal jpBet;
    @JsonProperty("JPPrize")
    public BigDecimal JpPrize;
    @JsonProperty("NetWin")
    public BigDecimal netWin;
    @JsonProperty("RequireAmt")
    public BigDecimal requireAmt;
    @JsonProperty("GameDate")
    public String gameDate;
    @JsonProperty("CreateDate")
    public String createDate;
    @JsonProperty("Ts")
    public Long ts;

}
