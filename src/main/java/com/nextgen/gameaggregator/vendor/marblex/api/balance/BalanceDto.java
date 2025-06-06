package com.nextgen.gameaggregator.vendor.marblex.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.marblex.dto.CommonDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto extends CommonDto {

    @NotBlank
    @JsonProperty("Currency")
    @Size(max = 5)
    private String currency;
}
