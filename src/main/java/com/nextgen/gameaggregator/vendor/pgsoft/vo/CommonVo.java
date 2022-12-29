package com.nextgen.gameaggregator.vendor.pgsoft.vo;

import lombok.Data;

import javax.annotation.Nullable;

@Data
public class CommonVo {
    // This variable will be null when there is no error.
    @Nullable
    private Integer error = null;
}
