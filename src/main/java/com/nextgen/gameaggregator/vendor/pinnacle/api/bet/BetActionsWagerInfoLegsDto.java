package com.nextgen.gameaggregator.vendor.pinnacle.api.bet;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetActionsWagerInfoLegsDto {
    @JsonProperty("SportId")
    private Long sportId;

    @JsonProperty("Sport")
    private String sport;

    @JsonProperty("SportGroup")
    private Long sportGroup;

    @JsonProperty("League")
    private String league;

    @JsonProperty("BetType")
    private Long betType;

    @JsonProperty("LeagueId")
    private long leagueId;

    @JsonProperty("EventId")
    private long eventId;

    @JsonProperty("EventDateFm")
    private String eventDateFm;

    @JsonProperty("SelectionType")
    private Long selectionType;

    @JsonProperty("InplayScore")
    private Long inplayScore;

    @JsonProperty("InPlay")
    private boolean inPlay;

    @JsonProperty("Odds")
    private BigDecimal odds;

    @JsonProperty("Handicap")
    private BigDecimal handicap;

    @JsonProperty("Period")
    private Long period;

    @JsonProperty("TeamType")
    private Long teamType;

    @JsonProperty("EventName")
    private String eventName;

    @JsonProperty("Selection")
    private String selection;
}
