package com.nextgen.gameaggregator.eventing.events;

import com.nextgen.gameaggregator.entity.BetInformation;
import com.nextgen.gameaggregator.entity.UnsettledBetResult;
import com.nextgen.gameaggregator.eventing.core.Event;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ResultBetEvent implements Event {
    private BetInformation betInformation;
    private BigDecimal lastBalance;
}
