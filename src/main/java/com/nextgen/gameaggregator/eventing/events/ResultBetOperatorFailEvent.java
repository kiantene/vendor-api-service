package com.nextgen.gameaggregator.eventing.events;

import com.nextgen.gameaggregator.entity.RawResultBet;
import com.nextgen.gameaggregator.entity.RawUnsettledBet;
import com.nextgen.gameaggregator.eventing.core.Event;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResultBetOperatorFailEvent implements Event {
    private RawResultBet rawResultBet;
    private Integer responseCode;
}
