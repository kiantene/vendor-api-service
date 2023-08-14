package com.nextgen.gameaggregator.vendor.bgaming.api.freespin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FreeSpinDto {
    @NotBlank
    @JsonProperty("issue_id")
    private String issueId;
    @NotBlank
    @JsonProperty("status")
    private String status;
    @JsonProperty("total_amount")
    private int totalAmount;
}
