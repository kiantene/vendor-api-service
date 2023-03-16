package com.nextgen.gameaggregator.eventing.events;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.RawUnsettledBet;
import com.nextgen.gameaggregator.eventing.core.Event;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UnsettledBetOperatorFailEvent implements Event {
    private RawUnsettledBet rawUnsettledBet;
    private Integer responseCode;
}
