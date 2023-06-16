package com.nextgen.gameaggregator.vendor.habanero.api.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.habanero.dto.BaseGameDto;
import com.nextgen.gameaggregator.vendor.habanero.dto.SubAuthDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransferDto {

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @JsonProperty("type")
    public String type;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9:._-]+$")
    @JsonProperty("dtsent")
    public String dtSent;

    @JsonProperty("tt")
    public String eventTrigger;

    @JsonProperty("basegame")
    public BaseGameDto baseGame;

    @JsonProperty("auth")
    public SubAuthDto subAuth;

    @JsonProperty("fundtransferrequest")
    public FundTransferRequestDto fundTransferRequestDto;
}
