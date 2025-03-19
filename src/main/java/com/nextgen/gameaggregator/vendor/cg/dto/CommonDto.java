package com.nextgen.gameaggregator.vendor.cg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonDto {

    private String version;

    @NotBlank
    private String channelId;

    @NotBlank
    private String data;

}
