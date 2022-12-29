package com.nextgen.gameaggregator.vendor.pgsoft.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import javax.annotation.Nullable;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CommonVo {
    // This variable will be null when there is no error.
    @Nullable
    private Integer error = null;
}
