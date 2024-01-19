package com.nextgen.gameaggregator.eventing.events;

import com.nextgen.gameaggregator.entity.ga.BetHistory;
import com.nextgen.gameaggregator.entity.ga.BetResultLog;
import com.nextgen.gameaggregator.eventing.core.Event;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class BetResultEvent implements Event {
    private BetHistory betHistory;
    private BetResultLog betResultLog;
    private BigDecimal lastBalance;
}
