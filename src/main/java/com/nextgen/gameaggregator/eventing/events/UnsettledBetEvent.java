package com.nextgen.gameaggregator.eventing.events;

import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.eventing.core.Event;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class UnsettledBetEvent implements Event {
    private UnsettledBet unsettledBet;
    private BigDecimal lastBalance;
}
