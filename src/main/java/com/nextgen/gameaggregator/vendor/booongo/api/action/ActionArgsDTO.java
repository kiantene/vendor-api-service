package com.nextgen.gameaggregator.vendor.booongo.api.action;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionArgsDTO {
    @JsonProperty("bonus")
    private Object bonus;
}
