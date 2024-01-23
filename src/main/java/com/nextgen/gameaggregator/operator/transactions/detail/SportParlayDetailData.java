package com.nextgen.gameaggregator.operator.transactions.detail;

import lombok.Data;

@Data
public class SportParlayDetailData extends MatchDetailData {

    private String betTypeName;
    private String odds;
    private String betStatus;
    private Long settleDate;

}
