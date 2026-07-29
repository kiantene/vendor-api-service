package com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.evoplay.dto.DetailsDataDto;
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
