package com.nextgen.gameaggregator.eventing.events;

import com.nextgen.gameaggregator.entity.SettledBet;
import com.nextgen.gameaggregator.eventing.core.Event;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SettledBetOperatorFailEvent implements Event {
    private SettledBet settledBet;
    private Integer responseCode;
}
