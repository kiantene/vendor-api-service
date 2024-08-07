package com.nextgen.gameaggregator.vendor.live22.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.live22.dto.CommonDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto extends CommonDto {
    @NotBlank
    @Size(max = 500)
    @JsonProperty("AuthToken")
    private String authToken; //authenticate and validate a player's game session
}
