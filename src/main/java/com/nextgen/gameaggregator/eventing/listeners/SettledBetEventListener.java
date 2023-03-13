package com.nextgen.gameaggregator.eventing.listeners;

import com.nextgen.gameaggregator.entity.RawResultBet;
import com.nextgen.gameaggregator.entity.RawSettledBet;
import com.nextgen.gameaggregator.eventing.core.EventListener;
import com.nextgen.gameaggregator.eventing.events.ResultBetEvent;
import com.nextgen.gameaggregator.eventing.events.SettledBetEvent;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.repository.RawResultBetRepository;
import com.nextgen.gameaggregator.repository.RawSettledBetRepository;
import com.nextgen.gameaggregator.repository.RawUnsettledBetRepository;
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
    @Autowired
    private RawResultBetRepository rawResultBetRepository;

    @Override
    public void onEvent(SettledBetEvent event) {

        RawSettledBet rawSettledBet = event.getRawSettledBet();
        cachingService.deleteUnsettledBetCaching(rawSettledBet.getRoundId(), rawSettledBet.getVendorGameId(),
                rawSettledBet.getVendorPlayerId());

        cachingService.deleteResultBetCaching(rawSettledBet.getRoundId(), rawSettledBet.getVendorGameId(),
                rawSettledBet.getVendorPlayerId());

        String Id = rawSettledBet.getRoundId()+'_'+rawSettledBet.getVendorGameId()+'_'+rawSettledBet.getVendorPlayerId();

        try {
            rawUnsettledBetRepository.deleteById(Id);
        } catch (EmptyResultDataAccessException e) {
            // Handle exception for document not found
            log.info("Unable to delete from rawUnsettledBet with ID " + Id);
        }

        try {
            rawResultBetRepository.deleteById(Id);
        } catch (EmptyResultDataAccessException e) {
            // Handle exception for document not found
            log.info("Unable to delete from rawResultBet with ID " + Id);
        }
    }
}
