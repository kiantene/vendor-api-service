package com.nextgen.gameaggregator.eventing.events;

import com.nextgen.gameaggregator.entity.SettledBet;
import com.nextgen.gameaggregator.eventing.core.Event;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class SettledBetEvent implements Event {
    private SettledBet settledBet;
    private BigDecimal lastBalance;
}
