package com.nextgen.gameaggregator.vendor.epicwin.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.epicwin.dto.CommonDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto extends CommonDto {
    @NotBlank
    @Size(max = 500)
    @JsonProperty("AuthToken")
    private String authToken; //authenticate and validate a player's game session
}
