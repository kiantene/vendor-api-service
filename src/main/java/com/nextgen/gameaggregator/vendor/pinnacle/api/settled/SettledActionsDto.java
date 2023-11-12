package com.nextgen.gameaggregator.vendor.pinnacle.api.settled;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.CommonDto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SettledActionsDto extends CommonDto {
    @JsonProperty("Actions")
    private List<SettledActions> actions;
}
