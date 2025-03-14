package com.nextgen.gameaggregator.entity.ga;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EndRoundSettledBetForPatching extends EndRoundSettledBet {

    private Integer sendToOperator = 1;

}
