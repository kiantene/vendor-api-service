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
     *
     * @deprecated for migration vendors that can receive a settlement before the round's
     * opening bet: a non-opening callback landing on v1 (e.g. during cutover config skew)
     * claims a round it never opened and pins it to v1 for the marker's lifetime.
     * Use {@link #markOnRoundOpen} on the round-opening action and {@link #touchMarker}
     * on every other v1 callback.
     */
    @Deprecated
    void markAsV1(String vendor, String roundId);

    /**
     * Claim: tag a round as handled by v1, called only for the round-opening transaction.
     * Creates the marker if absent, otherwise only slides its expiry — so {@code firstSeen}
     * records the round's first v1 callback rather than its most recent one.
     * Failures are swallowed: the v1 callback must not fail because of this.
     */
    void markOnRoundOpen(String vendor, String roundId);

    /**
     * Keep-alive: slide an existing marker's expiry so a round still receiving v1 traffic
     * is not un-pinned mid-flight. Never creates a marker — a round with no marker was not
     * opened on v1 and must stay routable to v2.
     * Failures are swallowed: the v1 callback must not fail because of this.
     */
    void touchMarker(String vendor, String roundId);

    /**
     * Lookup: used by the RouteResolver.
     * Returns the marker if present, empty otherwise.
     * On Couchbase failure, returns empty (caller decides default — typically v2).
     */
    Optional<RoundMarker> findMarker(String vendor, String roundId);

}
