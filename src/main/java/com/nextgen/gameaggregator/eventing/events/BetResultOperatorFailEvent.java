package com.nextgen.gameaggregator.eventing.events;

import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.eventing.core.Event;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BetResultOperatorFailEvent implements Event {
    private BetResultLog betResultLog;
    private Integer responseCode;
}
