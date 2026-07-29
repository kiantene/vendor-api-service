package com.nextgen.gameaggregator.vendor.jili.api.freespin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FreeSpinData {
    @NotBlank
    private String referenceId;
    private Integer remain;
    private Float originalBet;
    private Integer deduct;
    private Boolean skipRidValidation;
}
