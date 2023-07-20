package com.nextgen.gameaggregator.vendor.evoplay.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BasicDto {
    private String project;
    private String version;
}
