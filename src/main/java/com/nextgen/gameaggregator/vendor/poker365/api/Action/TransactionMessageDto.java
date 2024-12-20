package com.nextgen.gameaggregator.vendor.poker365.api.Action;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionMessageDto {
    @NotBlank
    @Size(max = 255)
    @JsonProperty("action")
    private String action;
}
