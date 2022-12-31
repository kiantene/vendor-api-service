package com.nextgen.gameaggregator.event;

import com.nextgen.gameaggregator.entity.BetHistory;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EndRoundEvent implements Event {
    private BetHistory betHistory;
}
