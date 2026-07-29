package com.nextgen.gameaggregator.entity.ga;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EndRoundSettledBetForPatching extends EndRoundSettledBet {

    private Integer sendToOperator = 1;
    private String operatorResultType = "END";
    private String vendorPlayerUsername;
    @JsonProperty("isRefund")
    private boolean isRefund;
    @JsonProperty("isSendToOperator")
    private boolean callToOperator = true;

}
