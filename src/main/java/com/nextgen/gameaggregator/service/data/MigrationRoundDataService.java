package com.nextgen.gameaggregator.service.data;

import com.nextgen.gameaggregator.entity.couchbase.RoundMarker;
import org.springframework.stereotype.Service;

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
public interface MigrationRoundDataService {

    /**
     * Shadow-write: tag a round as handled by v1.
     * Idempotent — safe to call on every v1 callback.
     * Failures are swallowed: the v1 callback must not fail because of this.
     */
    void markAsV1(String vendor, String roundId);

    /**
     * Lookup: used by the RouteResolver.
     * Returns the marker if present, empty otherwise.
     * On Couchbase failure, returns empty (caller decides default — typically v2).
     */
    Optional<RoundMarker> findMarker(String vendor, String roundId);

}
