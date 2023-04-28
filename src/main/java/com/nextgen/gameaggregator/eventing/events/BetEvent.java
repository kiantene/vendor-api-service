package com.nextgen.gameaggregator.eventing.events;

import com.nextgen.gameaggregator.entity.BetInformation;
import com.nextgen.gameaggregator.eventing.core.Event;
import lombok.Data;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class BetEvent implements Event {
    private BetInformation betInformation;
    private BigDecimal lastBalance;
}
