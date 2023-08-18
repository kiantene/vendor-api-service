package com.nextgen.gameaggregator.vendor.evolution.api.gameurl;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayerGroupDto {
    private String id;
    private String action;
}
