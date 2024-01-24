package com.nextgen.gameaggregator.eventing.listeners;

import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.eventing.core.EventListener;
import com.nextgen.gameaggregator.eventing.events.SettledBetEvent;
import com.nextgen.gameaggregator.repository.ga.writer.RawSettledBetRepository;
import com.nextgen.gameaggregator.repository.ga.writer.RawUnsettledBetRepository;
import com.nextgen.gameaggregator.service.CachingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SettledBetEventListener implements EventListener<SettledBetEvent> {

    @Autowired
    private RawSettledBetRepository rawSettledBetRepository;
    @Autowired
    private CachingService cachingService;
    @Autowired
    private RawUnsettledBetRepository rawUnsettledBetRepository;

    @Override
    public void onEvent(SettledBetEvent event) {

        SettledBet settledBet = event.getSettledBet();
        cachingService.deleteUnsettledBetCaching(settledBet.getVendorBetId(), settledBet.getRoundId(), settledBet.getVendorLineId(),
                settledBet.getVendorPlayerId());

        cachingService.deleteResultBetCaching(settledBet.getVendorBetId(), settledBet.getRoundId(), settledBet.getVendorLineId(),
                settledBet.getVendorPlayerId());

        cachingService.deleteUnsettledBetByGameIdCaching(settledBet.getVendorBetId(), settledBet.getRoundId(), settledBet.getVendorGameId(),
                settledBet.getVendorPlayerId());

        String Id = settledBet.getVendorBetId()+'_'+ settledBet.getRoundId()+'_'+ settledBet.getVendorLineId()+'_'+ settledBet.getVendorPlayerId();

        try {
            rawUnsettledBetRepository.deleteById(Id);
        } catch (EmptyResultDataAccessException e) {
            // Handle exception for document not found
            log.info("Unable to delete from rawUnsettledBet with ID " + Id);
        }
    }
}
