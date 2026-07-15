package com.nextgen.gameaggregator.vendor.lucky365.util;

import java.util.Locale;

/**
 * Lucky365 sends the {@code LoginId} back in callbacks with inconsistent casing (often
 * upper-cased). Our stored {@code vendor_players.username} is always lower-case and the
 * column collation is case-sensitive ({@code latin1_general_cs}), so a raw upper-cased
 * LoginId misses the DB on a cache miss and surfaces as {@code PlayerNotFound}.
 *
 * <p>Use {@link #forLookup(String)} for any player/credential lookup keyed by LoginId.
 * Never apply it to values fed into the signature hash — MD5 is byte-sensitive and the
 * vendor signs with the exact case it sends.
 */
public final class LoginIds {

    private LoginIds() {
    }

    /**
     * Normalises a vendor-supplied LoginId to the canonical form used to store and look up
     * players (lower-case). Returns {@code null} for a {@code null} input.
     *
     * <p>Deliberately lower-case only — matches the pre-existing mapper behaviour for backward
     * compatibility. (The cache key additionally trims, but real LoginIds carry no surrounding
     * whitespace, so trimming here would be a no-op.)
     */
    public static String forLookup(String loginId) {
        return loginId == null ? null : loginId.toLowerCase(Locale.ROOT);
    }
}
