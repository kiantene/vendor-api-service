package com.nextgen.gameaggregator.eventing.events;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.eventing.core.Event;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EndRoundEvent implements Event {
    private BetHistory betHistory;
}
