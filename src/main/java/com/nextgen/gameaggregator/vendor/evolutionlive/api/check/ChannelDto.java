package com.nextgen.gameaggregator.vendor.evolutionlive.api.check;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChannelDto {
    private String type;
}
