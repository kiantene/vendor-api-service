package com.nextgen.gameaggregator.vendor.ygg.api.getbalance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetBalanceDto {

    @NotBlank
    @Size(max = 255)
    private String org;

    @NotBlank
    @Size(max = 50)
    @JsonProperty("playerid")
    private String playerId;

}
