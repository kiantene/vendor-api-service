package com.nextgen.gameaggregator.entity.ga;

import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BetResultDlq {
    private BetResultData betResultData;
    private Long vendorPlayerId;
    private Integer agentId;
    private Integer vendorGameId;
    private String roundId;
}