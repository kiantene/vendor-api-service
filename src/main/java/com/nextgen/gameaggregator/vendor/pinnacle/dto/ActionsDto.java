package com.nextgen.gameaggregator.vendor.pinnacle.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ActionsDto extends CommonDto {
    @JsonProperty("Actions")
    private List<Action> actions;
}
