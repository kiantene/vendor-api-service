package com.nextgen.gameaggregator.eventing.events;

import com.nextgen.gameaggregator.entity.UnsettledBetResult;
import com.nextgen.gameaggregator.eventing.core.Event;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResultBetOperatorFailEvent implements Event {
    private UnsettledBetResult unsettledBetResult;
    private Integer responseCode;
}
