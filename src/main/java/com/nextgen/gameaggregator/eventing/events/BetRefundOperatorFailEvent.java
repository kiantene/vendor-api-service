package com.nextgen.gameaggregator.eventing.events;

import com.nextgen.gameaggregator.entity.BetRefundLog;
import com.nextgen.gameaggregator.eventing.core.Event;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BetRefundOperatorFailEvent  implements Event {
    private BetRefundLog betRefundLog;
    private Integer responseCode;
}
