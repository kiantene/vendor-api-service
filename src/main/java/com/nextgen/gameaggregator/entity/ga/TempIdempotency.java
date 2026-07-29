package com.nextgen.gameaggregator.entity.ga;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TempIdempotency {

    private String idempotencyKey;
    private Long createTimeEpochMillis;
    private Integer count;

    public static TempIdempotency ofNew(String key) {
        return new TempIdempotency(
                key,
                System.currentTimeMillis(),
                0
        );
    }
}
