package com.nextgen.gameaggregator.eventing.events;

import com.nextgen.gameaggregator.entity.ga.BetHistory;
import com.nextgen.gameaggregator.entity.ga.BetInformation;
import com.nextgen.gameaggregator.entity.ga.BetRefundLog;
import com.nextgen.gameaggregator.eventing.core.Event;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class BetRollbackEvent implements Event {
    private BetHistory betHistory;
    private BetInformation betInformation;
    private BetRefundLog betRefundLog;
    private BigDecimal lastBalance;
}
