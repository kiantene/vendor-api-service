package com.nextgen.gameaggregator.vendor.gpkpushgaming.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionDto {
    @NotBlank
    @Pattern(regexp = "TimeoutBetReturn|PointInout|CallBalance")
    private String cmd;
}
