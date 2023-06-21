package com.nextgen.gameaggregator.vendor.evoplay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DetailsDetailsDto {
    private String event_id;
    private DetailsDataDto data;
    private String time;
    private String date;
    private String type;
    private String system_id;
}
