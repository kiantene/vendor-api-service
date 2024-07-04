package com.nextgen.gameaggregator.entity.ga;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RawBatchProcessIdempotentLog {
    private String id;
    private String action;
    private String url;
}
