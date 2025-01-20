package com.nextgen.gameaggregator.vendor.koolbet.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonDto {

    @NotBlank
    public String reqId;

    @NotBlank
    public String token;
}
