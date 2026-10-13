package com.nextgen.gameaggregator.vendor.jdb.api;

/**
 * Validation group for the JDB amount precision limit ({@code @Digits(fraction = 6)}), sequenced
 * after the default group on each action DTO with {@code @GroupSequence}.
 *
 * <p>Why a separate group: {@code ValidationUtils.validateRequest} collects violations into a map
 * keyed by field name, and the validator returns them in no fixed order. A negative amount with
 * too many decimals fails both {@code @Positive} (8003) and {@code @Digits} (8000) on the same
 * field, so whichever came last would win, and the response code would vary between runs.
 * Checking precision only once the default constraints pass makes 8003 win every time.
 */
public interface AmountPrecisionCheck {
}
