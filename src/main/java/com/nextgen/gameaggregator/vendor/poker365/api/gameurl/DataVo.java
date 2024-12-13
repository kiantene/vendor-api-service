package com.nextgen.gameaggregator.vendor.poker365.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataVo {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String url;
}
