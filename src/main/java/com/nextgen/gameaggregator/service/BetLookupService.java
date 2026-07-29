package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.RawBetLookup;
import com.nextgen.gameaggregator.repository.ga.writer.RawBetLookupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Purpose
 * -------
 * This service exists to support rollback lookup using Couchbase KV operations instead of
 * secondary index / N1QL queries.
 *
 * Background
 * ----------
 * The actual bet documents (unsettled_bets and settled_bets collections) use a document ID
 * composed of multiple fields:
 *
 * - vendorBetId
 * - roundId
 * - vendorGameId
 * - vendorPlayerId
 *
 * However, during the rollback flow the system only receives:
 *
 * - vendorPlayerId
 * - externalTransactionId
 *
 * Because the full document key cannot be reconstructed from these two fields alone,
 * we store a helper document (RawBetLookup) that captures the full key components when
 * the bet is first processed.
 *
 * During rollback:
 * 1. We perform a KV lookup using (vendorPlayerId + externalTransactionId)
 * 2. Retrieve the stored key components
 * 3. Rebuild the actual document ID used by unsettled/settled bet collections
 * 4. Perform direct KV operations instead of index/query based lookups
 *
 * This avoids query latency and improves rollback performance under high TPS workloads.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BetLookupService {

    private final RawBetLookupRepository rawBetLookupRepository;

    public RawBetLookup save(String vendorBetId, String externalTransactionId, String roundId, Integer vendorGameId, Long vendorPlayerId) {
        try {
            RawBetLookup rawBetLookup = RawBetLookup.of(vendorBetId, externalTransactionId, roundId, vendorGameId, vendorPlayerId);
            return rawBetLookupRepository.save(rawBetLookup);
        } catch (Exception ex) { // suppress errors
            log.error("Failed to save RawBetLookup. vendorBetId={}, externalTransactionId={}, roundId={}, vendorGameId={}, vendorPlayerId={}", vendorBetId, externalTransactionId, roundId, vendorGameId, vendorPlayerId, ex);
        }
        return null;
    }

    public Optional<RawBetLookup> get(Long vendorPlayerId, String externalTransactionId) {
        String id = RawBetLookup.generateId(vendorPlayerId, externalTransactionId);
        return rawBetLookupRepository.findById(id);
    }
}
