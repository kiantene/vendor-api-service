package com.nextgen.gameaggregator.service.data.couchbase;

import com.nextgen.gameaggregator.entity.couchbase.ApiVersion;
import com.nextgen.gameaggregator.entity.couchbase.RoundMarker;
import com.nextgen.gameaggregator.repository.couchbase.MigrationRoundMarkerRepository;
import com.nextgen.gameaggregator.service.data.MigrationRoundDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Service for managing v1/v2 round version markers in Couchbase.
 *
 * Used by:
 *   - v1 callback handler: shadow-writes a marker for every v1 callback
 *   - RouteResolver: looks up the marker to decide v1 vs v2 routing
 *
 * Key format: <vendor>::<roundId>  (e.g. "evoplay::7517987443")
 */
@Service
public class CouchbaseMigrationRoundDataService implements MigrationRoundDataService {

    private static final Logger log = LoggerFactory.getLogger(CouchbaseMigrationRoundDataService.class);

    /**
     * Marker retention. Not externalised on purpose: the v1/v2 router is temporary and retires
     * with the migration, so there is no operational reason to retune this per environment.
     * Must not exceed the maxTTL on the game.migration_round collection (10 days) — Couchbase
     * silently caps a larger per-document expiry, which would make a bigger value here a lie.
     */
    private static final Duration TTL = Duration.ofDays(10);

    private final MigrationRoundMarkerRepository repository;
//    private final MigrationMetrics metrics;

    public CouchbaseMigrationRoundDataService(MigrationRoundMarkerRepository repository) {
        this.repository = repository;
//        this.metrics = metrics;
    }

    /**
     * Shadow-write: tag a round as handled by v1.
     * Idempotent — safe to call on every v1 callback.
     * Failures are swallowed: the v1 callback must not fail because of this.
     */
    @Deprecated
    public void markAsV1(String vendor, String roundId) {
        String key = buildKey(vendor, roundId);
        try {
            RoundMarker marker = new RoundMarker(ApiVersion.V1, Instant.now().toEpochMilli());
            repository.upsert(key, marker, TTL);
//            metrics.increment("v1_marker_write_success");
        } catch (Exception e) {
            log.error("Failed to write v1 marker for key={}", key, e);
//            metrics.increment("v1_marker_write_failure");
            // swallow — never break the v1 callback
        }
    }

    /**
     * Claim: tag a round as handled by v1, called only for the round-opening transaction.
     * Creates the marker if absent, otherwise only slides its expiry.
     * Failures are swallowed: the v1 callback must not fail because of this.
     */
    @Override
    public void markOnRoundOpen(String vendor, String roundId) {
        String key = buildKey(vendor, roundId);
        try {
            RoundMarker marker = new RoundMarker(ApiVersion.V1, Instant.now().toEpochMilli());

            // Two passes: the marker can appear between our insert and our touch, or expire
            // between them. Either way the intended end state is "a marker exists with a fresh
            // expiry", so a lost race is retried once rather than reported as a failed claim.
            for (int attempt = 0; attempt < 2; attempt++) {
                if (repository.insertIfAbsent(key, marker, TTL)) {
                    return;
                }
                if (repository.touchIfPresent(key, TTL)) {
                    return;
                }
            }

            // Both passes lost the race in opposite directions. Nothing was thrown, so without
            // this the round would end up unpinned as quietly as the bug this guards against.
            //
            // Deliberately not forced with an upsert: that would put body-overwriting semantics
            // back into the claim path, resetting firstSeen on every contended claim.
            log.warn("Could not claim v1 marker for key={} - insert and touch both lost the race "
                    + "twice; round is left unpinned and may route to v2", key);
        } catch (Exception e) {
            log.error("Failed to claim v1 marker for key={}", key, e);
            // swallow — never break the v1 callback
        }
    }

    /**
     * Keep-alive: slide an existing marker's expiry. Never creates one, so a round that was
     * not opened on v1 stays routable to v2.
     * Failures are swallowed: the v1 callback must not fail because of this.
     */
    @Override
    public void touchMarker(String vendor, String roundId) {
        String key = buildKey(vendor, roundId);
        try {
            // A missing marker is the normal case for a v2-owned round, not a problem: the
            // return value is ignored precisely because there is nothing to report.
            repository.touchIfPresent(key, TTL);
        } catch (Exception e) {
            log.error("Failed to extend v1 marker for key={}", key, e);
            // swallow — never break the v1 callback
        }
    }

    /**
     * Lookup: used by the RouteResolver.
     * Returns the marker if present, empty otherwise.
     * On Couchbase failure, returns empty (caller decides default — typically v2).
     */
    public Optional<RoundMarker> findMarker(String vendor, String roundId) {
        String key = buildKey(vendor, roundId);
        try {
            return repository.get(key);
        } catch (Exception e) {
            log.error("Marker lookup failed for key={}", key, e);
//            metrics.increment("router_lookup_failure");
            return Optional.empty();
        }
    }

    private String buildKey(String vendor, String roundId) {
        return vendor + "::" + roundId;
    }
}
