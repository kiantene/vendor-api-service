package com.nextgen.gameaggregator.vendor.db.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonDto {

    @NotBlank
    public String agent;

    @NotBlank
    public String timestamp;

    @NotBlank
    @Size(max = 1000)
    public String sign;

}
