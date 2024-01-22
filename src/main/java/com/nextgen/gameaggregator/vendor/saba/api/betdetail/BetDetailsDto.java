package com.nextgen.gameaggregator.vendor.saba.api.betdetail;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class BetDetailsDto {

    private String transId;
    private String vendorMemberId;
    private String operatorId;
    private String leagueId;
    private List<LangNameDto> leaguename;
    private String matchId;
    private String homeId;
    private List<LangNameDto> hometeamname;
    private String awayId;
    private List<LangNameDto> awayteamname;
    private String matchDatetime;
    private String sportType;
    private List<LangNameDto> sportname;
    private String betType;
    @SerializedName("bettypename")
    private List<LangNameDto> betTypeName;
    private String parlayRefNo;
    private String odds;
    private String stake;
    private String transactionTime;
    private String ticketStatus;
    private String buybackAmount;
    private String winlostAmount;
    private String afterAmount;
    private String currency;
    private String winlostDatetime;
    private String oddsType;
    private String betTeam;
    @SerializedName("isLucky")
    private String isLucky;
    private String ticketExtraStatus;
    private String parlayType;
    private String comboType;
    private String homeHdp;
    private String awayHdp;
    private String hdp;
    private String betfrom;
    private String islive;
    private String homeScore;
    private String awayScore;
    private String settlementTime;
    @SerializedName("customInfo1")
    private String customInfo1;
    @SerializedName("customInfo2")
    private String customInfo2;
    @SerializedName("customInfo3")
    private String customInfo3;
    @SerializedName("customInfo4")
    private String customInfo4;
    @SerializedName("customInfo5")
    private String customInfo5;
    private String baStatus;
    private String versionKey;
    @SerializedName("ParlayData")
    private List<ParlayDataDto> parlayData;
    private String risklevelname;
    private String risklevelnamecs;
}
