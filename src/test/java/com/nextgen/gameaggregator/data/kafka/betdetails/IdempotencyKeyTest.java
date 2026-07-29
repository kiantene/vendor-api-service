package com.nextgen.gameaggregator.data.kafka.betdetails;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyKeyTest {

    @Test
    void formatsAsVendorColonBetIdColonEventKind() {
        String key = IdempotencyKey.of("pinnacle", "12345", EventKind.PLACE_BET);
        assertThat(key).isEqualTo("pinnacle:12345:PLACE_BET");
    }

    @Test
    void differentEventKindsProduceDifferentKeys() {
        String placeKey = IdempotencyKey.of("saba", "R1", EventKind.PLACE_BET);
        String updateKey = IdempotencyKey.of("saba", "R1", EventKind.UPDATE_BET);
        String resultKey = IdempotencyKey.of("saba", "R1", EventKind.RESULT_UPDATE);

        assertThat(placeKey).isNotEqualTo(updateKey);
        assertThat(updateKey).isNotEqualTo(resultKey);
        assertThat(placeKey).isNotEqualTo(resultKey);
    }

    @Test
    void nullVersionFallsBackToUnversionedKey() {
        String versioned = IdempotencyKey.of("pinnacle", "W1", EventKind.RESULT_UPDATE, null);
        String unversioned = IdempotencyKey.of("pinnacle", "W1", EventKind.RESULT_UPDATE);
        assertThat(versioned).isEqualTo(unversioned);
    }

    @Test
    void pinnacleResettleKeyDoesNotCollideWithOriginalSettle() {
        String originalSettleKey = IdempotencyKey.of("pinnacle", "W1", EventKind.RESULT_UPDATE);
        String resettleKey = IdempotencyKey.of("pinnacle", "W1", EventKind.RESULT_UPDATE, 987654321L);

        assertThat(resettleKey).isEqualTo("pinnacle:W1:RESULT_UPDATE:v987654321");
        assertThat(originalSettleKey).isNotEqualTo(resettleKey);
    }
}
