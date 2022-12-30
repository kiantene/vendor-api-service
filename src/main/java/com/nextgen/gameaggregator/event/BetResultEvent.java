package com.nextgen.gameaggregator.event;

import com.nextgen.gameaggregator.entity.BetResultLog;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BetResultEvent implements Event {
    private BetResultLog data;
}
