package com.nextgen.gameaggregator.vendor.evolutionlive.api.check;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.evolutionlive.dto.BasicDto;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckDto extends BasicDto {
    private ChannelDto channel;

}
