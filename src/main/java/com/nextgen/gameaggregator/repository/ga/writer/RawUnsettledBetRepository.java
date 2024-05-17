package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Scope("raw")
@Collection("unsettled_bet")
public interface RawUnsettledBetRepository extends CouchbaseRepository<UnsettledBet, String> {

    void deleteById(String Id);

    @Cacheable(value = "UnsettledBet", key = "{#vendorPlayerId, #externalTransactionId}", cacheManager = "cacheManager")
    UnsettledBet findByVendorPlayerIdAndExternalTransactionId(Long vendorPlayerId, String externalTransactionId);

    @Cacheable(value = "UnsettledBet", key = "{#externalTransactionId, #vendorId}", cacheManager = "cacheManager")
    UnsettledBet findByVendorIdAndExternalTransactionId(Integer vendorId, String externalTransactionId);

    List<UnsettledBet> findByRoundIdAndVendorGameIdAndVendorPlayerIdOrderByCreateTime(String roundId, Integer vendorGameId, Long vendorPlayerId);

    @Cacheable(value = "UnsettledBet", key = "{#vendorBetId, #roundId, #vendorGameId, #vendorPlayerId}", cacheManager = "cacheManager")
    UnsettledBet findByRoundIdAndVendorBetIdAndVendorGameIdAndVendorPlayerId(String roundId, String vendorBetId, Integer vendorGameId, Long vendorPlayerId);

    //TODO ADD CACHEABLE
    UnsettledBet findTop1ByRoundIdAndVendorGameIdAndVendorPlayerIdOrderByCreateTimeDesc(String roundId, Integer vendorGameId, Long vendorPlayerId);
}
