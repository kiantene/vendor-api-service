package com.nextgen.gameaggregator.vendor.pgsoft.api.endround;

import com.nextgen.gameaggregator.eventing.core.EventDispatcherSystem;
import com.nextgen.gameaggregator.eventing.events.BetResultEvent;
import com.nextgen.gameaggregator.eventing.events.EndRoundEvent;
import com.nextgen.gameaggregator.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EndRoundService {
    public void process (BetResultEvent betResultEvent) {
        EventDispatcherSystem.emitAsync(new EndRoundEvent(betResultEvent.getBetHistory()));
    }

}
