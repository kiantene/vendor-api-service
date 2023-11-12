package com.nextgen.gameaggregator.vendor.pinnacle.api.accept;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AcceptActionsWagerInfoDto {
    @JsonProperty("WagerId")
    private Long wagerId;

    @JsonProperty("Type")
    private String type;

    @JsonProperty("BetType")
    private Long betType;

    @JsonProperty("Odds")
    private BigDecimal odds;

    @JsonProperty("OddsFormat")
    private Long oddsFormat;

    @JsonProperty("ToWin")
    private BigDecimal toWin;

    @JsonProperty("ToRisk")
    private BigDecimal toRisk;

    @JsonProperty("Stake")
    private BigDecimal stake;

    @JsonProperty("Period")
    private Long period;

    @JsonProperty("Selection")
    private String selection;

    @JsonProperty("EventId")
    private Long eventId;

    @JsonProperty("EventName")
    private String eventName;

    @JsonProperty("EventDateFm")
    private String eventDateFm;

    @JsonProperty("LeagueId")
    private Long leagueId;

    @JsonProperty("SportId")
    private Long sportId;

    @JsonProperty("Sport")
    private String sport;

    @JsonProperty("Inplay")
    private Boolean inplay;

    @JsonProperty("InPlayScore")
    private String inPlayScore;

    @JsonProperty("Handicap")
    private BigDecimal handicap;

    @JsonProperty("SelectionType")
    private String selectionType;

    @JsonProperty("LeagueName")
    private String leagueName;

    @JsonProperty("ParentEventName")
    private String parentEventName;

    @JsonProperty("PlayerIPAddress")
    private String playerIPAddress;

    @JsonProperty("WagerMasterId")
    private Long wagerMasterId;

    @JsonProperty("WagerNum")
    private Integer wagerNum;

    @JsonProperty("RoundRobinOptions")
    private String roundRobinOptions;
}
