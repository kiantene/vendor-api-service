package com.nextgen.gameaggregator.vendor.pinnacle.api.bet;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.CommonDto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BetActionsDto extends CommonDto {
    @JsonProperty("Actions")
    private List<BetActions> actions;
}
