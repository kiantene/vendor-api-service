package com.nextgen.gameaggregator.eventing.events;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.BetInformation;
import com.nextgen.gameaggregator.entity.BetRefundLog;
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
