package com.nextgen.gameaggregator.vendor.yesbingo.api.action;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionDto {

    // an id to identify which end point is triggered
    @NotNull
    @Digits(fraction = 0, integer = 32)
    @Positive
    public Integer action;

}
