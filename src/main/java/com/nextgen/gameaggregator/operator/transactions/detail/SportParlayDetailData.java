package com.nextgen.gameaggregator.operator.transactions.detail;

import lombok.Data;

@Data
public class SportParlayDetailData {

    private String homeTeam;
    private String awayTeam;
    private String matchName;
    private Long matchDate;
    private String homeScore;
    private String awayScore;

}
