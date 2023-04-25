package com.nextgen.gameaggregator.vendor.hacksawgaming.api.betdetail;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UrlVo {
    private String record;
    private String recordType;
}
