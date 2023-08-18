package com.nextgen.gameaggregator.vendor.evolution.api.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChannelDto {
    private String type; // "M" for mobile clients, "P" for all other
}
