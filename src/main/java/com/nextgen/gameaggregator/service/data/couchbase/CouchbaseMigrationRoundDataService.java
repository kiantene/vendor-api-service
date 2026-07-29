package com.nextgen.gameaggregator.service.data.couchbase;

import com.nextgen.gameaggregator.entity.couchbase.ApiVersion;
import com.nextgen.gameaggregator.entity.couchbase.RoundMarker;
import com.nextgen.gameaggregator.repository.couchbase.MigrationRoundMarkerRepository;
import com.nextgen.gameaggregator.service.data.MigrationRoundDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private final MigrationRoundMarkerRepository repository;
//    private final MigrationMetrics metrics;
    private final Duration ttl;

    public CouchbaseMigrationRoundDataService(
            MigrationRoundMarkerRepository repository,
//            MigrationMetrics metrics,
            @Value("${migration.marker.ttl-days:30}") int ttlDays) {
        this.repository = repository;
//        this.metrics = metrics;
        this.ttl = Duration.ofDays(ttlDays);
    }

    /**
     * Shadow-write: tag a round as handled by v1.
     * Idempotent — safe to call on every v1 callback.
     * Failures are swallowed: the v1 callback must not fail because of this.
     */
    public void markAsV1(String vendor, String roundId) {
        String key = buildKey(vendor, roundId);
        try {
            RoundMarker marker = new RoundMarker(ApiVersion.V1, Instant.now().toEpochMilli());
            repository.upsert(key, marker, ttl);
//            metrics.increment("v1_marker_write_success");
        } catch (Exception e) {
            log.error("Failed to write v1 marker for key={}", key, e);
//            metrics.increment("v1_marker_write_failure");
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
