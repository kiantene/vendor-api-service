package com.nextgen.gameaggregator.eventing.events;

import com.nextgen.gameaggregator.entity.RawResultBet;
import com.nextgen.gameaggregator.entity.RawUnsettledBet;
import com.nextgen.gameaggregator.eventing.core.Event;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ResultBetEvent implements Event {
    private RawResultBet rawResultBet;
    private BigDecimal lastBalance;
}
