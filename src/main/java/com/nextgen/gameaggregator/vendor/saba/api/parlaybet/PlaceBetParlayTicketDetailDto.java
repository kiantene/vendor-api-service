package com.nextgen.gameaggregator.vendor.saba.api.parlaybet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaceBetParlayTicketDetailDto {
    private Integer oddsType;
}
