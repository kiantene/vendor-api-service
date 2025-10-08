package com.nextgen.gameaggregator.core.service;

import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.service.UnsettledBetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegacyCleanupService {
    private final UnsettledBetService unsettledBetService;

    public void cleanup(GameRound round,
                        String vendorBetId,
                        Integer vendorGameId,
                        Long vendorPlayerId) {

        if (vendorBetId == null) return;

        UnsettledBet unsettledBet = new UnsettledBet();
        unsettledBet.setVendorId(round.getVendorId());
        unsettledBet.setVendorBetId(vendorBetId);
        unsettledBet.setRoundId(round.getRoundId());
        unsettledBet.setVendorGameId(vendorGameId);
        unsettledBet.setVendorPlayerId(vendorPlayerId);
        unsettledBet.setId(unsettledBet.generateId());

        try {
            unsettledBetService.delete(unsettledBet);
        } catch (Exception ex) {
            log.error("Unsettled bet (" + unsettledBet.getId() + ") cannot be deleted: " + ex.getMessage());
        }
    }
}
