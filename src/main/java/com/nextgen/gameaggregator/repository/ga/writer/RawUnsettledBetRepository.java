package com.nextgen.gameaggregator.repository.ga.writer;

import com.couchbase.client.java.query.QueryScanConsistency;
import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.couchbase.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Scope("raw")
@Collection("unsettled_bet")
public interface RawUnsettledBetRepository extends CouchbaseRepository<UnsettledBet, String> {

    void deleteById(String Id);

    @Cacheable(value = "UnsettledBetByETID", key = "{#vendorPlayerId, #externalTransactionId}", cacheManager = "cacheManager", unless = "#result == null")
    UnsettledBet findByVendorPlayerIdAndExternalTransactionId(Long vendorPlayerId, String externalTransactionId);

    @Cacheable(value = "UnsettledBet", key = "{#externalTransactionId, #vendorId}", cacheManager = "cacheManager", unless = "#result == null")
    UnsettledBet findByVendorIdAndExternalTransactionId(Integer vendorId, String externalTransactionId);

    List<UnsettledBet> findByRoundIdAndVendorGameIdAndVendorPlayerIdOrderByCreateTime(String roundId, Integer vendorGameId, Long vendorPlayerId);

    @Cacheable(value = "UnsettledBetTop1", key = "{#roundId, #vendorGameId, #vendorPlayerId}", cacheManager = "cacheManager", unless = "#result == null")
    UnsettledBet findTop1ByRoundIdAndVendorGameIdAndVendorPlayerIdOrderByCreateTimeDesc(String roundId, Integer vendorGameId, Long vendorPlayerId);

    @Cacheable(value = "UnsettledBetTop1", key = "{#roundId}", cacheManager = "cacheManager", unless = "#result == null")
    UnsettledBet findTop1ByRoundIdAndVendorGameIdIsNotNullAndVendorPlayerIdIsNotNullOrderByCreateTimeDesc(String roundId, Integer vendorGameId, Long vendorPlayerId);

    List<UnsettledBet> findByRoundId(String roundId);

    @Query("#{#n1ql.selectEntity} WHERE vendorPlayerId = $vendorPlayerId AND externalTransactionId = $externalTransactionId")
    @ScanConsistency(query = QueryScanConsistency.REQUEST_PLUS)
    UnsettledBet findByVendorPlayerIdAndExternalTransactionIdWithRequestPlus(
            @Param("vendorPlayerId") Long vendorPlayerId,
            @Param("externalTransactionId") String externalTransactionId
    );
}
