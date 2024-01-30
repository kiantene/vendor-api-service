package com.nextgen.gameaggregator.custodianseamless.service;


import com.nextgen.gameaggregator.entity.ga.AgentPlayer;
import com.nextgen.gameaggregator.entity.ga.Currency;
import com.nextgen.gameaggregator.entity.ga.RawTransferHistory;
import com.nextgen.gameaggregator.repository.ga.writer.RawTransferHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransferHistoryService {

    @Autowired
    private RawTransferHistoryRepository rawTransferHistoryRepository;


    @Cacheable(value = "RawTransferHistories", key = "{#referenceId, #agentId}", cacheManager = "cacheManager", unless = "#result == null")
    public RawTransferHistory getTransactionHistoryById(String referenceId, Integer  agentId) {
        return rawTransferHistoryRepository.findByReferenceIdAndAgentId(referenceId, agentId);
    }

    @CachePut(value = "RawTransferHistories", key = "{#referenceId, #agentPlayer.agentId}", cacheManager = "cacheManager")
    public RawTransferHistory preGenerateRawTransferHistory(String referenceId, AgentPlayer agentPlayer, Currency currency, Integer transactionType, BigDecimal transferAmount){
        return new RawTransferHistory(referenceId, agentPlayer, currency, transactionType, transferAmount);
    }
    @CachePut(value = "RawTransferHistories", key = "{#rawTransferHistory.referenceId, #rawTransferHistory.agentId}", cacheManager = "cacheManager")
    public RawTransferHistory updateRawTransferHistory(RawTransferHistory rawTransferHistory){
        return rawTransferHistory;
    }

    public void saveRawTransferHistory(RawTransferHistory rawTransferHistory){
        rawTransferHistoryRepository.save(rawTransferHistory) ;
    }

}
