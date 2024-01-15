package com.nextgen.gameaggregator.eventing.events;

import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.eventing.core.Event;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UnsettledBetOperatorFailEvent implements Event {
    private UnsettledBet unsettledBet;
    private Integer responseCode;
}
