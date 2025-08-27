package com.nextgen.gameaggregator.core.service;

import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.entity.warehouse.BetHistory;
import com.nextgen.gameaggregator.service.SettledBetService;
import com.nextgen.gameaggregator.service.WarehouseBetHistoryService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettledBetDataService {
    private final SettledBetService settledBetService;
    private final WarehouseBetHistoryService warehouseBetHistoryService;

    public boolean prepareSettledBets(String betId, Long timestamp) {
        List<BetHistory> betHistoryList = this.findSettledBets(betId, timestamp);
        if (betHistoryList == null || betHistoryList.isEmpty()) return false;

        List<SettledBet> settledBetList = buildSettledBetDocuments(betHistoryList);
        storeSettledBetDocuments(settledBetList);

        return true;
    }

    private List<BetHistory> findSettledBets(String betId, Long timestamp) {
//        return warehouseBetHistoryService
//                .findByExternalTransactionIdAndVendorSettleTime(betId, timestamp);
        return List.of();
    }

    private List<SettledBet> buildSettledBetDocuments(List<BetHistory> betHistoryList) {
        long createTime = System.currentTimeMillis();

        return betHistoryList.stream()
                .map(betHistory -> mapToSettledBet(betHistory, createTime))
                .toList();
    }

    private SettledBet mapToSettledBet(BetHistory betHistory, long createTime) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        SettledBet settledBet = modelMapper.map(betHistory, SettledBet.class);
        settledBet.setBetId(betHistory.getId());
        settledBet.setCreateTime(createTime);
        return settledBet;
    }

    private void storeSettledBetDocuments(List<SettledBet> settledBetList) {
//        try {
//            settledBetService.saveAll(settledBetList);
//        } catch (Exception ex) {
//            // TODO: store failure in couchbase rollback_dlq
//            throw ex;
//        }
    }
}
