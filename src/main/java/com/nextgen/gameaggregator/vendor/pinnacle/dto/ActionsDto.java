package com.nextgen.gameaggregator.vendor.pinnacle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ActionsDto extends CommonDto {
    @JsonProperty("Actions")
    @NotEmpty(message = "Actions cannot be null or empty")
    private List<Action> actions;
}
