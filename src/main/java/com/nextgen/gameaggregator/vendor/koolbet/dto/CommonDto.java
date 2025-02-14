package com.nextgen.gameaggregator.vendor.koolbet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonDto {

    @NotBlank
    @Size(max = 255)
    public String reqId;

    @NotBlank
    @Size(max = 255)
    public String token;
}
