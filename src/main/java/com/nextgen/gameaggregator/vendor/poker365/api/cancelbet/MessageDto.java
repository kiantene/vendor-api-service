package com.nextgen.gameaggregator.vendor.poker365.api.cancelbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageDto {
    @NotBlank
    @Size(max = 255)
    @JsonProperty("userId")
    private String userId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("gameNumber")
    private String gameNumber;

}
