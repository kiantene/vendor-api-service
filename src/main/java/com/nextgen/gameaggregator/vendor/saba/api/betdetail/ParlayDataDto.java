package com.nextgen.gameaggregator.vendor.saba.api.betdetail;

import lombok.Data;

import java.util.List;

@Data
public class ParlayDataDto {
    private String parlayId;
    private String leagueId;
    private List<LangNameDto> leaguename;
    private String matchId;
    private String homeId;
    private List<LangNameDto> hometeamname;
    private String awayId;
    private List<LangNameDto> awayteamname;
    private String matchDatetime;
    private String odds;
    private String betType;
    private List<LangNameDto> bettypename;
    private String betTeam;
    private String sportType;
    private List<LangNameDto> sportname;
    private String homeHdp;
    private String awayHdp;
    private String hdp;
    private String islive;
    private String homeScore;
    private String awayScore;
    private String ticketStatus;
    private String winlostDatetime;
}
