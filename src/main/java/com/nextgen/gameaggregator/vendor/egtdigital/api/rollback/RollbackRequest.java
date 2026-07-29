package com.nextgen.gameaggregator.vendor.egtdigital.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.egtdigital.dto.RequestCommonDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackRequest extends RequestCommonDto {

    @NotBlank
    @Size(max = 255)
    @JsonProperty("transferId")
    private String transferId;

    @Size(max = 255)
    @NotBlank
    @JsonProperty("referenceId")
    private String referenceId;

    @Size(max = 255)
    @NotBlank
    @JsonProperty("roundNumber")
    private String roundNumber;


}
