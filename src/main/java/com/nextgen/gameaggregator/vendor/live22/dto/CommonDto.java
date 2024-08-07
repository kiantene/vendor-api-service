package com.nextgen.gameaggregator.vendor.live22.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonDto {
    @NotBlank
    @Size(max = 255)
    @JsonProperty("OperatorId")
    private String operatorId; //credential that provided by vendor
    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
    @JsonProperty("RequestDateTime")
    private String requestDateTime; //for MD5 hashing purposes

    @NotBlank
    @Size(max = 1000)
    @JsonProperty("Signature")
    private String signature;

    @NotBlank
    @Size(max = 50)
    @JsonProperty("PlayerId")
    private String playerId;

    @NotBlank
    @Size(max = 5)
    @JsonProperty("Currency")
    private String currency;

}
