package com.nextgen.gameaggregator.vendor.pinnacle.api.bet;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetActionsWagerInfoLegsDto {
    private Long SportId;
    private String Sport;
    private Long SportGroup;
    private String League;
    private Long BetType;
    private long LeagueId;
    private long EventId;
    private String EventDateFm;
    private Long SelectionType;
    private Long InplayScore;
    private boolean InPlay;
    private BigDecimal Odds;
    private BigDecimal Handicap;
    private Long Period;
    private Long TeamType;
    private String EventName;
    private String Selection;
}
