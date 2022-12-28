package com.nextgen.gameaggregator.vendor.pgsoft.vo;

import brave.internal.Nullable;
import lombok.Data;

@Data
public class CommonVo {
    // This variable will be null when there is no error.
    @Nullable
    private Integer error = null;
}
