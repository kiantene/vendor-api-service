package com.nextgen.gameaggregator.vendor.pinnacle.api.bet;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetActionsWagerInfoDto {
    private Long WagerId;
    private String Type;
    private Long BetType;
    private BigDecimal Odds;
    private Long OddsFormat;
    private BigDecimal ToWin;
    private BigDecimal ToRisk;
    private BigDecimal Stake;
    private Long Period;
    private String Selection;
    private Long EventId;
    private String EventName;
    private String EventDateFm;
    private Long LeagueId;
    private Long SportId;
    private String Sport;
    private Boolean Inplay;
    private String InPlayScore;
    private BigDecimal Handicap;
    private String SelectionType;
    private String LeagueName;
    private String ParentEventName;
    private String PlayerIPAddress;
    private Long WagerMasterId;
    private Integer WagerNum;
    private String RoundRobinOptions;
}
