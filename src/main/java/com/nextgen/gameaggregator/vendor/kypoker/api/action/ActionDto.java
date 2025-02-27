package com.nextgen.gameaggregator.vendor.kypoker.api.action;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionDto {
    Integer s;
}
